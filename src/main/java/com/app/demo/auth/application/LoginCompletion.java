package com.app.demo.auth.application;

import com.app.demo.auth.config.AuthProperties;
import com.app.demo.auth.domain.TokenProvider;
import com.app.demo.user.domain.DeviceInfo;
import com.app.demo.user.domain.DeviceState;
import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserInfoRepository;
import com.app.demo.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** 로그인 최종 성공 처리: 오류 횟수 초기화 → 마지막 로그인 시각 → 기기 등록/갱신 → Access-Token 발급 + 세션 정보 */
@Component
@RequiredArgsConstructor
class LoginCompletion {

	private final UserRepository users;
	private final UserInfoRepository infos;
	private final TokenProvider tokens;
	private final AuthProperties authProps;
	private final Clock clock;

	/** 1단계에서 바로 완료(등록 기기 + loginType=MESSAGE) */
	LoginSuccess complete(User user, int loginType, DeviceInfo device, DeviceState state) {
		String accessToken = finish(user, device);
		return new LoginSuccess(session(user, loginType, state), accessToken);
	}

	/** OTP/임시비밀번호 단계를 거쳐 완료. 이 시점에 기기가 등록되므로 deviceState 는 MATCH 다. */
	LoginSuccess completeVerified(User user, DeviceInfo device) {
		String accessToken = finish(user, device);
		return new LoginSuccess(session(user, user.loginTypeCode(), DeviceState.MATCH), accessToken);
	}

	private String finish(User user, DeviceInfo device) {
		LocalDateTime now = LocalDateTime.now(clock);
		users.resetFailCount(user.userId());
		infos.markLogin(user.userId(), now);
		infos.registerDevice(user.userId(), device, now);
		return tokens.createAccessToken(user.userId(), user.userType(), device.deviceId());
	}

	/** 로그인 완료 응답용 세션 정보 (비밀번호 만료로 변경이 필요한지 포함) */
	private SessionResult session(User user, @Nullable Integer loginType, DeviceState state) {
		boolean required = user.passwordChangeRequired(LocalDateTime.now(clock), authProps.passwordExpireDays());
		return new SessionResult(user.userId(), loginType, user.userType(), state, required, user.lastChangePw());
	}
}
