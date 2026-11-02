package com.app.demo.auth.application;

/** OTP 검증 성공 결과: 세션 정보 + Access-Token. */
public record VerifyResult(LoginResult.SessionResult session, String accessToken) {
}
