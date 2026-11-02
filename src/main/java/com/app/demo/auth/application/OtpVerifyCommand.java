package com.app.demo.auth.application;

/** OTP 검증 요청. */
public record OtpVerifyCommand(String challengeToken, String channel, String otp) {
}
