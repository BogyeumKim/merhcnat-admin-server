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
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * 로그인 1단계: id/pw + 기기정보 검증. 비밀번호는 이 단계에서만 받는다.
 * <ul>
 *   <li>비밀키로 확인된 등록 기기 + loginType=MESSAGE → OTP 없이 바로 로그인 완료</li>
 *   <li>그 외(loginType=TOTP 는 항상, 기기 비밀키가 없거나 틀린 경우 포함) → MFA 챌린지 토큰 안내(MFA_REQUIRED)</li>
 *   <li>임시비밀번호·만료된 비밀번호 사용자 → 비밀번호 변경 챌린지 안내(PASSWORD_CHANGE_REQUIRED)</li>
 * </ul>
 * 세 경우 모두 이 단계의 성공이라 HTTP 200 이고, 실패(id/pw 오류·잠금 등)만 4xx 예외로 던진다.
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
		DeviceInfo device = cmd.device();
		AuthAuditContext.subject(IdMasking.mask(cmd.userId()), Sha256.hex(device.deviceId()));
		User user = authenticate(cmd.userId(), cmd.password());
		LoginType type = user.loginType().orElseThrow(() -> new BusinessException(AuthErrorCode.UNSUPPORTED_LOGIN_TYPE));
		UserInfo info = infos.findOrCreate(user.userId());

		boolean otpSkippable = type == LoginType.MESSAGE && info.isTrustedDevice(device.deviceId(), cmd.deviceSecret());
		if (!otpSkippable) {
			return mfaRequired(user, info, type, device);
		}
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
		String challengeToken = challenges.issueMfa(user.userId(), device);
		DeviceNotice notice = deviceNotice(user.userId(), info, device);
		MfaChallenge challenge = new MfaChallenge(user.userId(), type.getCode(), user.userType(), notice.state(),
				channelOptions(available, user), lastUsedChannel(info, available), challengeToken,
				notice.registeredUserIdMasked(), notice.registeredToOtherAccount());
		return new LoginStep.MfaRequired(challenge);
	}

	/** 수단별 마스킹된 수신처 */
	private static List<ChannelOption> channelOptions(List<OtpChannel> available, User user) {
		return available.stream().map(channel -> ChannelOption.of(channel, user)).toList();
	}

	/** 마지막으로 성공한 수단. 지금은 쓸 수 없는 수단(연락처 삭제 등)이면 null */
	private static @Nullable OtpChannel lastUsedChannel(UserInfo info, List<OtpChannel> available) {
		String last = info.lastOtpChannel();
		return last == null ? null : OtpChannel.parse(last).filter(available::contains).orElse(null);
	}

	/** 비밀번호 변경용 챌린지를 발급해 이유와 함께 안내한다. */
	private LoginStep.PasswordChangeRequired passwordChangeRequired(User user, DeviceInfo device, PasswordChangeReason reason) {
		return new LoginStep.PasswordChangeRequired(challenges.issuePasswordChange(user.userId(), device, reason));
	}

	/**
	 * 기기 안내 정보.
	 * <ul>
	 *   <li>MATCH  : 이 사용자에 등록된 기기와 같음(비밀키가 없거나 틀려 OTP 를 요구하는 경우 포함)</li>
	 *   <li>CHANGED: 본인의 다른 기기가 등록돼 있음 → 본인 아이디(마스킹) / 이 기기가 다른 계정 소유 → 다른 계정 표시만(아이디 비공개)</li>
	 *   <li>NONE   : 최초</li>
	 * </ul>
	 */
	private DeviceNotice deviceNotice(String userId, UserInfo info, DeviceInfo device) {
		if (info.hasDevice(device.deviceId())) {
			return DeviceNotice.of(DeviceState.MATCH);
		}
		String owner = infos.findUserIdByDeviceId(device.deviceId());
		if (owner != null && !owner.equals(userId)) {
			return new DeviceNotice(DeviceState.CHANGED, null, Boolean.TRUE);
		}
		if (info.deviceId() != null) {
			return new DeviceNotice(DeviceState.CHANGED, IdMasking.mask(userId), null);
		}
		return DeviceNotice.of(DeviceState.NONE);
	}

	/** MFA 안내에 실을 기기 상태와 부가 표시(응답에서 null 은 제외된다) */
	private record DeviceNotice(DeviceState state, @Nullable String registeredUserIdMasked, @Nullable Boolean registeredToOtherAccount) {

		static DeviceNotice of(DeviceState state) {
			return new DeviceNotice(state, null, null);
		}
	}
}
