package com.app.demo.auth.application;

import com.app.demo.auth.domain.OtpChannel;
import com.app.demo.user.domain.DeviceState;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** login() 결과: 즉시 완료 또는 MFA 필요. 프레젠테이션 계층이 HTTP status 로 매핑한다. */
public sealed interface LoginResult {

	/** 로그인 완료. accessToken 은 Access-Token 응답 헤더로 내려간다. */
	record Completed(SessionResult session, String accessToken) implements LoginResult {
	}

	/** id/pw 는 통과했지만 추가 인증(OTP)이 필요하다. */
	record MfaRequired(ChallengeResult challenge) implements LoginResult {
	}

	/** 로그인된 세션 정보 */
	record SessionResult(String userId, @Nullable Integer loginType, @Nullable Integer userType, DeviceState deviceState) {
	}

	/** MFA 안내: 사용 가능한 수단과 이후 단계에 쓸 챌린지 토큰 */
	record ChallengeResult(
			String userId,
			int loginType,
			@Nullable Integer userType,
			DeviceState deviceState,
			List<OtpChannel> channels,
			String challengeToken) {
	}
}
