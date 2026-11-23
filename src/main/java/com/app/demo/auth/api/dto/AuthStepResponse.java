package com.app.demo.auth.api.dto;

/** 로그인 단계 성공(200) 응답. 앱은 {@code step}(SUCCESS / MFA_REQUIRED / PASSWORD_CHANGE_REQUIRED)으로 다음 화면을 고른다. */
public sealed interface AuthStepResponse permits AuthSessionResponse, MfaChallengeResponse, PasswordChangeRequiredResponse {

	String getStep();
}
