package com.app.demo.auth.api;

import com.app.demo.auth.api.dto.AuthSessionResponse;
import com.app.demo.auth.api.dto.AuthStepResponse;
import com.app.demo.auth.api.dto.MfaChallengeResponse;
import com.app.demo.auth.api.dto.PasswordChangeRequiredResponse;
import com.app.demo.auth.application.LoginStep;
import com.app.demo.global.security.AuthAuditContext;
import lombok.experimental.UtilityClass;

/** 로그인 단계 결과를 200 응답으로 옮긴다. 완료면 세션, 아니면 다음 단계(MFA·비밀번호 변경) 안내다. */
@UtilityClass
class LoginStepResponse {

	/** ex) {@code MfaRequired} → {@code {step: MFA_REQUIRED, challengeToken: ...}} */
	static AuthStepResponse from(LoginStep step) {
		return switch (step) {
			case LoginStep.Completed completed -> completed(completed);
			case LoginStep.MfaRequired mfa -> mfaRequired(mfa);
			case LoginStep.PasswordChangeRequired change -> passwordChangeRequired(change);
		};
	}

	private static AuthStepResponse completed(LoginStep.Completed completed) {
		return AuthSessionResponse.from(completed.getSession(), completed.getAccessToken());
	}

	private static AuthStepResponse mfaRequired(LoginStep.MfaRequired mfa) {
		MfaChallengeResponse response = MfaChallengeResponse.from(mfa.getChallenge());
		return nextStep(response);
	}

	private static AuthStepResponse passwordChangeRequired(LoginStep.PasswordChangeRequired change) {
		PasswordChangeRequiredResponse response = PasswordChangeRequiredResponse.from(change.getChallenge());
		return nextStep(response);
	}

	private static AuthStepResponse nextStep(AuthStepResponse response) {
		AuthAuditContext.result(response.getStep()); // 200 이지만 로그인 미완료 단계임을 감사 로그에 구분해 남긴다
		return response;
	}
}
