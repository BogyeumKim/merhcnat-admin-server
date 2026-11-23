package com.app.demo.auth.application;

import com.app.demo.auth.domain.OtpChannel;
import com.app.demo.user.domain.DeviceState;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/** MFA 안내: 사용 가능한 수단과 이후 단계에 쓸 챌린지 토큰 (200 MFA_REQUIRED 응답의 data). */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MfaChallenge {

	private final String userId;

	private final int loginType;

	private final @Nullable Integer userType;

	/** NONE | MATCH | CHANGED */
	private final DeviceState deviceState;

	/** 사용 가능한 수단과 마스킹된 수신처 (loginType 0 → TOTP, 1 → 연락처가 있는 KAKAO, SMS, EMAIL 순) */
	private final List<ChannelOption> channels;

	/** 마지막으로 인증에 성공한 수단. 이력이 없거나 지금 쓸 수 없는 수단이면 null(응답에서 제외) */
	private final @Nullable OtpChannel lastUsedChannel;

	/** 이후 send/verify 에 쓰는 5분짜리 서명 토큰(일회용) */
	private final String challengeToken;

	/** deviceState=CHANGED 일 때 이 기기에 현재 등록된 아이디(마스킹): 본인의 다른 기기면 본인, 다른 계정 소유 기기면 그 계정. 그 외 null(응답에서 제외) */
	private final @Nullable String registeredUserIdMasked;

	/** 이 기기가 다른 계정에 등록돼 있으면 true(registeredUserIdMasked 는 그 계정 아이디). 그 외 null(응답에서 제외) */
	private final @Nullable Boolean deviceRegisteredToOtherAccount;
}
