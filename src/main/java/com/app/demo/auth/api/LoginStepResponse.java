package com.app.demo.auth.api;

import com.app.demo.auth.api.dto.AuthSessionResponse;
import com.app.demo.auth.api.dto.AuthStepResponse;
import com.app.demo.auth.api.dto.MfaChallengeResponse;
import com.app.demo.auth.api.dto.PasswordChangeRequiredResponse;
import com.app.demo.auth.application.LoginStep;
import com.app.demo.auth.application.MfaChallenge;
import com.app.demo.auth.application.PasswordChangeChallenge;
import com.app.demo.global.api.ApiResponse;
import com.app.demo.global.security.AuthAuditContext;
import lombok.experimental.UtilityClass;

/** 로그인 단계 결과를 200 응답으로 옮긴다. 완료면 SUCCESS, 아니면 다음 단계 코드(MFA_REQUIRED·PASSWORD_CHANGE_REQUIRED)다. */
@UtilityClass
class LoginStepResponse {

	/** ex) {@code MfaRequired} → {@code {code: MFA_REQUIRED, data: {challengeToken...}}} */
	static ApiResponse<AuthStepResponse> from(LoginStep step) {
		return switch (step) {
			case LoginStep.Completed completed -> completed(completed);
			case LoginStep.MfaRequired mfa -> mfaRequired(mfa.getChallenge());
			case LoginStep.PasswordChangeRequired change -> passwordChangeRequired(change.getChallenge());
		};
	}

	private static ApiResponse<AuthStepResponse> completed(LoginStep.Completed completed) {
		AuthSessionResponse data = AuthSessionResponse.from(completed.getSession(), completed.getAccessToken());
		return ApiResponse.ok(data);
	}

	private static ApiResponse<AuthStepResponse> mfaRequired(MfaChallenge challenge) {
		MfaChallengeResponse data = MfaChallengeResponse.from(challenge);
		return nextStep(AuthStepCode.MFA_REQUIRED, AuthStepCode.MFA_REQUIRED.getMessage(), data);
	}

	private static ApiResponse<AuthStepResponse> passwordChangeRequired(PasswordChangeChallenge challenge) {
		String message = AuthStepCode.passwordChangeMessage(challenge.getReason());
		PasswordChangeRequiredResponse data = PasswordChangeRequiredResponse.from(challenge);
		return nextStep(AuthStepCode.PASSWORD_CHANGE_REQUIRED, message, data);
	}

	private static ApiResponse<AuthStepResponse> nextStep(AuthStepCode code, String message, AuthStepResponse data) {
		AuthAuditContext.result(code.name()); // 200 이지만 로그인 미완료 단계임을 감사 로그에 구분해 남긴다
		return ApiResponse.of(code.name(), message, data);
	}
}
