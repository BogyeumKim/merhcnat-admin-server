package com.app.demo.auth.api.dto;

/** 로그인 단계 성공(200) 응답 data. 응답 code(SUCCESS / MFA_REQUIRED / PASSWORD_CHANGE_REQUIRED)에 따라 구현 타입이 정해진다. */
public sealed interface AuthStepResponse permits AuthSessionResponse, MfaChallengeResponse, PasswordChangeRequiredResponse {
}
