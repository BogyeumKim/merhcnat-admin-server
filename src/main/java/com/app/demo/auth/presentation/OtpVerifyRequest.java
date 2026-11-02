package com.app.demo.auth.presentation;

import org.jspecify.annotations.Nullable;

/** OTP 검증 요청. channel: TOTP | KAKAO | SMS */
public record OtpVerifyRequest(@Nullable String challengeToken, @Nullable String channel, @Nullable String otp) {
}
