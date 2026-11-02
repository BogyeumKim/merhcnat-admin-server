package com.app.demo.auth.application;

/** OTP 발송 요청 (비밀번호 대신 챌린지 토큰). */
public record OtpSendCommand(String challengeToken, String channel) {
}
