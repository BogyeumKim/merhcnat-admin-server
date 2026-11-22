package com.app.demo.auth.application;

import com.app.demo.auth.config.AuthProperties;
import com.app.demo.auth.domain.AccessToken;
import com.app.demo.auth.domain.TokenProvider;
import com.app.demo.global.security.Sha256;
import com.app.demo.user.domain.DeviceInfo;
import com.app.demo.user.domain.DeviceState;
import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserInfoRepository;
import com.app.demo.user.domain.UserRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * 로그인 최종 성공 처리: 오류 횟수 초기화 → 마지막 로그인 시각 → 기기 등록/갱신 → Access-Token 발급 → 로그인 세션(Redis) 교체 + 세션 정보.
 * OTP(또는 비밀번호 변경)를 거쳐 기기를 등록할 때 기기 비밀키를 새로 발급해 응답으로 한 번만 내려주고, DB 에는 해시만 저장한다.
 */
@Component
@RequiredArgsConstructor
class LoginCompletion {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int DEVICE_SECRET_BYTES = 32;

	private final UserRepository users;
	private final UserInfoRepository infos;
	private final TokenProvider tokens;
	private final AuthProperties authProps;
	private final SessionService sessions;
	private final Clock clock;

	/** 1단계에서 바로 완료(비밀키로 확인된 등록 기기 + loginType=MESSAGE). 기기 비밀키는 그대로 유지한다. */
	LoginStep.Completed complete(User user, int loginType, DeviceInfo device, DeviceState state) {
		String accessToken = finish(user, device, null);
		return new LoginStep.Completed(session(user, loginType, state, null), accessToken);
	}

	/** OTP/비밀번호 변경 단계를 거쳐 완료. 이 시점에 기기가 등록되므로 deviceState 는 MATCH 이고 새 기기 비밀키를 발급한다. */
	LoginStep.Completed completeVerified(User user, DeviceInfo device) {
		String deviceSecret = newDeviceSecret();
		String accessToken = finish(user, device, Sha256.hex(deviceSecret));
		return new LoginStep.Completed(session(user, user.loginTypeCode(), DeviceState.MATCH, deviceSecret), accessToken);
	}

	private String finish(User user, DeviceInfo device, @Nullable String deviceSecretHash) {
		LocalDateTime now = LocalDateTime.now(clock);
		users.resetFailCount(user.userId());
		infos.markLogin(user.userId(), now);
		infos.registerDevice(user.userId(), device, deviceSecretHash, now);
		String deviceIdHash = Sha256.hex(device.deviceId());
		AccessToken token = tokens.createAccessToken(user.userId(), user.userType(), deviceIdHash);
		sessions.start(user.userId(), token, deviceIdHash); // 기존 세션(다른 기기 토큰) 교체
		return token.value();
	}

	/** 로그인 완료 응답용 세션 정보. 만료된 비밀번호는 완료 전에 변경을 강제하므로 passwordChangeRequired 는 보통 false 다. */
	private SessionResult session(User user, @Nullable Integer loginType, DeviceState state, @Nullable String deviceSecret) {
		boolean required = user.isPasswordExpired(LocalDateTime.now(clock), authProps.passwordExpireDays());
		return new SessionResult(user.userId(), loginType, user.userType(), state, required, user.lastChangePw(), deviceSecret);
	}

	/** 256bit 난수 → base64url(패딩 없음) */
	private static String newDeviceSecret() {
		byte[] bytes = new byte[DEVICE_SECRET_BYTES];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
