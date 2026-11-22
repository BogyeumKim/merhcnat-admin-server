package com.app.demo.auth.application;

import com.app.demo.auth.domain.IdMasking;
import com.app.demo.auth.domain.OtpChannel;
import com.app.demo.auth.exception.AuthErrorCode;
import com.app.demo.global.api.ErrorData;
import com.app.demo.global.exception.BusinessException;
import com.app.demo.user.domain.DeviceInfo;
import com.app.demo.user.domain.DeviceState;
import com.app.demo.user.domain.LoginType;
import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserInfo;
import com.app.demo.user.domain.UserInfoRepository;
import com.app.demo.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 로그인 1단계: id/pw + 기기정보 검증. 비밀번호는 이 단계에서만 받는다.
 * <ul>
 *   <li>등록 기기(MATCH) + loginType=MESSAGE → OTP 없이 바로 로그인 완료</li>
 *   <li>그 외(loginType=TOTP 는 항상) → MFA 챌린지 토큰을 담아 401 MFA_REQUIRED 예외</li>
 *   <li>임시비밀번호 사용자 → 비밀번호 변경 챌린지를 담아 403 PASSWORD_CHANGE_REQUIRED 예외</li>
 * </ul>
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

	/** 로그인을 완료하면 결과를 반환하고, 추가 단계가 필요하면 예외(401/403)를 던진다. */
	public LoginSuccess login(LoginCommand cmd) {
		DeviceInfo device = cmd.device();
		User user = authenticate(cmd.userId(), cmd.password());
		LoginType type = user.loginType().orElseThrow(() -> new BusinessException(AuthErrorCode.UNSUPPORTED_LOGIN_TYPE));
		DeviceState state = deviceState(user.userId(), device);

		boolean otpSkippable = type == LoginType.MESSAGE && state == DeviceState.MATCH;
		if (!otpSkippable) {
			throw mfaRequired(user, type, state, device);
		}
		if (user.tempPassword()) {
			throw passwordChangeRequired(user, device);
		}
		return completion.complete(user, type.getCode(), device, state);
	}

	/** id/pw 검증. 없는 아이디와 틀린 비밀번호는 같은 응답으로 처리해 아이디 존재 여부를 숨긴다. */
	private User authenticate(String userId, String password) {
		User user = users.findById(userId).orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));
		accountLock.checkLocked(user);
		if (!user.passwordMatches(password)) {
			throw accountLock.recordFailure(userId,
					remaining -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS, ErrorData.remaining(remaining)));
		}
		accountLock.checkUsable(user); // 비밀번호가 맞은 뒤에만 정지/삭제 여부를 알려 준다 (아이디 존재 여부 노출 방지)
		return user; // 실패 횟수는 최종 로그인 성공 때만 초기화한다
	}

	/** OTP 단계용 챌린지를 발급하고, 그 안내를 data 에 담은 401 MFA_REQUIRED 예외를 만든다. */
	private BusinessException mfaRequired(User user, LoginType type, DeviceState state, DeviceInfo device) {
		String challengeToken = challenges.issueMfa(user.userId(), device);
		String registeredUserIdMasked = state == DeviceState.CHANGED ? registeredUserIdMasked(user.userId(), device) : null;
		MfaChallenge challenge = new MfaChallenge(user.userId(), type.getCode(), user.userType(), state,
				OtpChannel.forLoginType(type), challengeToken, registeredUserIdMasked);
		return new BusinessException(AuthErrorCode.MFA_REQUIRED, challenge);
	}

	/** 비밀번호 변경용 챌린지를 발급하고, 그 토큰을 data 에 담은 403 PASSWORD_CHANGE_REQUIRED 예외를 만든다. */
	private BusinessException passwordChangeRequired(User user, DeviceInfo device) {
		return new BusinessException(AuthErrorCode.PASSWORD_CHANGE_REQUIRED, challenges.issuePasswordChange(user.userId(), device));
	}

	/**
	 * 기기 상태 판별.
	 * MATCH: 이 사용자에 등록된 기기와 같음 / CHANGED: 다른 기기가 등록돼 있거나 이 기기가 다른 계정 소유 / NONE: 최초
	 */
	private DeviceState deviceState(String userId, DeviceInfo device) {
		UserInfo info = infos.findOrCreate(userId);
		if (info.hasDevice(device.deviceId())) {
			return DeviceState.MATCH;
		}
		if (info.deviceId() != null || infos.existsByDeviceId(device.deviceId())) {
			return DeviceState.CHANGED;
		}
		return DeviceState.NONE;
	}

	/** 기기 변경 안내용: 이 기기에 현재 등록된 아이디(마스킹). 다른 계정 소유가 아니면 본인 아이디. */
	private String registeredUserIdMasked(String userId, DeviceInfo device) {
		String owner = infos.findUserIdByDeviceId(device.deviceId());
		return IdMasking.mask(owner == null ? userId : owner);
	}
}
