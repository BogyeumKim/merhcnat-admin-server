package com.app.demo.auth.application;

import com.app.demo.auth.domain.OtpChannel;
import com.app.demo.user.domain.DeviceState;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * MFA 안내: 사용 가능한 수단과 이후 단계에 쓸 챌린지 토큰. 401 MFA_REQUIRED 응답의 data 로 그대로 직렬화된다.
 *
 * @param deviceState            NONE | MATCH | CHANGED
 * @param channels               사용 가능한 인증 수단 (loginType 0 → TOTP, 1 → KAKAO, SMS)
 * @param challengeToken         이후 send/verify 에 쓰는 5분짜리 서명 토큰(일회용)
 * @param registeredUserIdMasked deviceState=CHANGED 일 때만: 이 기기에 현재 등록된 아이디(마스킹). 그 외 null(응답에서 제외)
 */
public record MfaChallenge(
		String userId,
		int loginType,
		@Nullable Integer userType,
		DeviceState deviceState,
		List<OtpChannel> channels,
		String challengeToken,
		@Nullable String registeredUserIdMasked) {
}
