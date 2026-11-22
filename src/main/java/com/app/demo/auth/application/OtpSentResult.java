package com.app.demo.auth.application;

import org.jspecify.annotations.Nullable;

/**
 * OTP 발송 결과.
 *
 * @param maskedDestination 마스킹된 수신처 (휴대폰 또는 이메일)
 * @param maskedPhone       [전환 기간] KAKAO/SMS 일 때만 maskedDestination 과 같은 값, EMAIL 이면 null
 */
public record OtpSentResult(long expiresInSeconds, long resendAfterSeconds, String maskedDestination,
		@Nullable String maskedPhone) {
}
