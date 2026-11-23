package com.app.demo.auth.application;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/** OTP 발송 결과. */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OtpSentResult {

	private final long expiresInSeconds;

	private final long resendAfterSeconds;

	/** 이 챌린지에서 더 보낼 수 있는 횟수 (0 이면 다음 발송은 OTP_SEND_LIMIT_EXCEEDED) */
	private final int remainingSends;

	/** 마스킹된 수신처 (휴대폰 또는 이메일) */
	private final String maskedDestination;

	/** [전환 기간] KAKAO/SMS 일 때만 maskedDestination 과 같은 값, EMAIL 이면 null */
	private final @Nullable String maskedPhone;
}
