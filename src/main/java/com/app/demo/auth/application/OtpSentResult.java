package com.app.demo.auth.application;

import org.jspecify.annotations.Nullable;

/**
 * OTP 발송 결과.
 *
 * @param remainingSends    이 챌린지에서 더 보낼 수 있는 횟수 (0 이면 다음 발송은 OTP_SEND_LIMIT_EXCEEDED)
 * @param maskedDestination 마스킹된 수신처 (휴대폰 또는 이메일)
 * @param maskedPhone       [전환 기간] KAKAO/SMS 일 때만 maskedDestination 과 같은 값, EMAIL 이면 null
 */
public record OtpSentResult(long expiresInSeconds, long resendAfterSeconds, int remainingSends, String maskedDestination,
		@Nullable String maskedPhone) {
}
