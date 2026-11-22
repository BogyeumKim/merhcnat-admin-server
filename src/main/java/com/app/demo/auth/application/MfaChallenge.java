package com.app.demo.auth.application;

import com.app.demo.auth.domain.OtpChannel;
import com.app.demo.user.domain.DeviceState;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * MFA 안내: 사용 가능한 수단과 이후 단계에 쓸 챌린지 토큰 (200 MFA_REQUIRED 응답의 data).
 *
 * @param deviceState            NONE | MATCH | CHANGED
 * @param channels               사용 가능한 수단과 마스킹된 수신처 (loginType 0 → TOTP, 1 → 연락처가 있는 KAKAO, SMS, EMAIL 순)
 * @param lastUsedChannel        마지막으로 인증에 성공한 수단. 이력이 없거나 지금 쓸 수 없는 수단이면 null(응답에서 제외)
 * @param challengeToken         이후 send/verify 에 쓰는 5분짜리 서명 토큰(일회용)
 * @param registeredUserIdMasked deviceState=CHANGED 이고 본인의 다른 기기가 등록돼 있을 때만: 본인 아이디(마스킹). 그 외 null(응답에서 제외)
 * @param deviceRegisteredToOtherAccount 이 기기가 다른 계정에 등록돼 있으면 true. 다른 계정의 아이디는 내려주지 않는다. 그 외 null(응답에서 제외)
 */
public record MfaChallenge(
		String userId,
		int loginType,
		@Nullable Integer userType,
		DeviceState deviceState,
		List<ChannelOption> channels,
		@Nullable OtpChannel lastUsedChannel,
		String challengeToken,
		@Nullable String registeredUserIdMasked,
		@Nullable Boolean deviceRegisteredToOtherAccount) {
}
