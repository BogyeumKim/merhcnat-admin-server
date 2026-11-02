package com.app.demo.auth.presentation;

import com.app.demo.auth.application.LoginResult;
import com.app.demo.auth.domain.OtpChannel;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * MFA 필요(202, code=MFA_REQUIRED) data.
 *
 * @param deviceState    NONE | MATCH | CHANGED
 * @param channels       사용 가능한 인증 수단 (loginType 0 → TOTP, 1 → KAKAO, SMS)
 * @param challengeToken 이후 send/verify 에 쓰는 5분짜리 서명 토큰(일회용)
 */
public record MfaChallengeResponse(
		String userId,
		int loginType,
		@Nullable Integer userType,
		String deviceState,
		List<String> channels,
		String challengeToken) {

	static MfaChallengeResponse from(LoginResult.ChallengeResult c) {
		List<String> channels = c.channels().stream().map(OtpChannel::name).toList();
		return new MfaChallengeResponse(c.userId(), c.loginType(), c.userType(), c.deviceState().name(), channels, c.challengeToken());
	}
}
