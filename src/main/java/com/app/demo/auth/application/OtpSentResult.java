package com.app.demo.auth.application;

/** OTP 발송 결과. */
public record OtpSentResult(long expiresInSeconds, long resendAfterSeconds, String maskedPhone) {
}
