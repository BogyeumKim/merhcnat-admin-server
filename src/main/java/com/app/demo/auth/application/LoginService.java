package com.app.demo.auth.application;

import com.app.demo.auth.domain.IdMasking;
import com.app.demo.auth.domain.OtpChannel;
import com.app.demo.auth.exception.AuthErrorCode;
import com.app.demo.global.exception.BusinessException;
import com.app.demo.global.security.AuthAuditContext;
import com.app.demo.global.security.Sha256;
import com.app.demo.user.domain.DeviceInfo;
import com.app.demo.user.domain.DeviceState;
import com.app.demo.user.domain.LoginType;
import com.app.demo.user.domain.PasswordChangeReason;
import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserInfo;
import com.app.demo.user.domain.UserInfoRepository;
import com.app.demo.user.domain.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * 로그인 1단계: id/pw + 기기정보 검증(비밀번호는 이 단계에서만 받는다). 비밀키로 확인된 등록 기기 + loginType=MESSAGE 면 바로 로그인 완료,
 * 그 외(TOTP 는 항상, 비밀키가 없거나 틀림 포함)는 MFA_REQUIRED, 임시·만료 비밀번호는 PASSWORD_CHANGE_REQUIRED. 셋 다 200 이고 실패만 4xx 예외다.
 * 실패 횟수가 롤백되면 안 되므로 의도적으로 {@code @Transactional} 을 쓰지 않는다.
 */
@Service
@RequiredArgsConstructor
public class LoginService {

	private final UserRepository users;
	private final UserInfoRepository infos;
	private final AccountLockService accountLock;
	private final ChallengeService challenges;
	private final LoginCompletion completion;
	private final PasswordChangePolicy passwordChangePolicy;

	/** id/pw 가 맞으면 다음 단계(로그인 완료 / OTP 필요 / 비밀번호 변경 필요)를 반환한다. */
	public LoginStep login(LoginCommand cmd) {
		DeviceInfo device = cmd.getDevice();
		String maskedUserId = IdMasking.mask(cmd.getUserId());
		String deviceIdHash = Sha256.hex(device.getDeviceId());
		AuthAuditContext.subject(maskedUserId, deviceIdHash);

		// ── [1단계] 계정: id/pw → 이용 가능 → 로그인 유형 ──
		User user = authenticate(cmd.getUserId(), cmd.getPassword());
		LoginType type = user.loginType().orElseThrow(() -> new BusinessException(AuthErrorCode.UNSUPPORTED_LOGIN_TYPE));
		UserInfo info = infos.findOrCreate(user.getUserId());

		// ── [2단계] 기기: 비밀키로 확인된 등록 기기가 아니면 OTP ──
		boolean otpSkippable = type == LoginType.MESSAGE && info.isTrustedDevice(device.getDeviceId(), cmd.getDeviceSecret());
		if (!otpSkippable) {
			return mfaRequired(user, info, type, device);
		}

		// ── [3단계] 완료: 비밀번호 변경 필요 → 로그인 완료 ──
		Optional<PasswordChangeReason> changeReason = passwordChangePolicy.reasonFor(user);
		if (changeReason.isPresent()) {
			return passwordChangeRequired(user, device, changeReason.get());
		}
		return completion.complete(user, type.getCode(), device, DeviceState.MATCH);
	}

	/**
	 * id/pw 검증. 없는 아이디·틀린 비밀번호·삭제 계정은 status/code/data 까지 똑같은 401 로 응답해 아이디 존재 여부를 숨긴다.
	 * 잠금(423)·정지(403)는 비밀번호가 맞았을 때만 알려 준다.
	 * 주의: 비밀번호를 해시(bcrypt 등)로 바꾸면 없는 아이디에도 더미 해시 비교를 넣어 응답 시간을 맞춘다.
	 */
	private User authenticate(String userId, String password) {
		User user = users.findById(userId).orElseThrow(AccountLockService::invalidCredentials);
		accountLock.verifyLoginPassword(user, password);
		accountLock.checkUsable(user);
		return user; // 실패 횟수는 최종 로그인 성공 때 0 으로 초기화한다
	}

	/**
	 * OTP 단계용 챌린지를 발급하고 사용 가능한 수단을 안내한다.
	 * 연락처가 하나도 없으면 channels 를 빈 목록으로 내려준다(이후 otp/send 는 400 OTP_CHANNEL_INVALID).
	 */
	private LoginStep.MfaRequired mfaRequired(User user, UserInfo info, LoginType type, DeviceInfo device) {
		List<OtpChannel> available = OtpChannel.availableFor(type, user);
		String challengeToken = challenges.issueMfa(user.getUserId(), device);
		DeviceNotice notice = deviceNotice(user.getUserId(), info, device);
		List<ChannelOption> channels = channelOptions(available, user);
		OtpChannel lastUsedChannel = lastUsedChannel(info, available);
		MfaChallenge challenge = MfaChallenge.with()
				.userId(user.getUserId())
				.loginType(type.getCode())
				.userType(user.getUserType())
				.deviceState(notice.getState())
				.channels(channels)
				.lastUsedChannel(lastUsedChannel)
				.challengeToken(challengeToken)
				.registeredUserIdMasked(notice.getRegisteredUserIdMasked())
				.deviceRegisteredToOtherAccount(notice.getRegisteredToOtherAccount())
				.create();
		return LoginStep.MfaRequired.of(challenge);
	}

	/** 수단별 마스킹된 수신처 */
	private static List<ChannelOption> channelOptions(List<OtpChannel> available, User user) {
		return available.stream().map(channel -> ChannelOption.of(channel, user)).toList();
	}

	/** 마지막으로 성공한 수단. 지금은 쓸 수 없는 수단(연락처 삭제 등)이면 null */
	private static @Nullable OtpChannel lastUsedChannel(UserInfo info, List<OtpChannel> available) {
		String last = info.getLastOtpChannel();
		return last == null ? null : OtpChannel.parse(last).filter(available::contains).orElse(null);
	}

	/** 비밀번호 변경용 챌린지를 발급해 이유와 함께 안내한다. */
	private LoginStep.PasswordChangeRequired passwordChangeRequired(User user, DeviceInfo device, PasswordChangeReason reason) {
		PasswordChangeChallenge challenge = challenges.issuePasswordChange(user.getUserId(), device, reason);
		return LoginStep.PasswordChangeRequired.of(challenge);
	}

	/**
	 * 기기 안내 정보. MATCH: 이 사용자에 등록된 기기와 같음(비밀키가 없거나 틀려 OTP 를 요구하는 경우 포함) / NONE: 최초
	 * / CHANGED: 본인의 다른 기기가 등록돼 있음 → 본인 아이디(마스킹), 이 기기가 다른 계정 소유 → 다른 계정 표시 + 그 계정 아이디(마스킹)
	 */
	private DeviceNotice deviceNotice(String userId, UserInfo info, DeviceInfo device) {
		if (info.hasDevice(device.getDeviceId())) {
			return DeviceNotice.of(DeviceState.MATCH);
		}
		String owner = infos.findUserIdByDeviceId(device.getDeviceId());
		if (owner != null && !owner.equals(userId)) {
			return DeviceNotice.with()
					.state(DeviceState.CHANGED)
					.registeredUserIdMasked(IdMasking.mask(owner))
					.registeredToOtherAccount(Boolean.TRUE)
					.create();
		}
		if (info.getDeviceId() != null) {
			return DeviceNotice.with()
					.state(DeviceState.CHANGED)
					.registeredUserIdMasked(IdMasking.mask(userId))
					.create();
		}
		return DeviceNotice.of(DeviceState.NONE);
	}

	/** MFA 안내에 실을 기기 상태와 부가 표시(응답에서 null 은 제외된다) */
	@Getter
	@Builder(builderMethodName = "with", buildMethodName = "create")
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	private static class DeviceNotice {

		private final DeviceState state;

		private final @Nullable String registeredUserIdMasked;

		private final @Nullable Boolean registeredToOtherAccount;

		static DeviceNotice of(DeviceState state) {
			return DeviceNotice.with().state(state).create();
		}
	}
}
