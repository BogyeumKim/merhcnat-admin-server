package com.app.demo.auth.application;

/**
 * 로그인 단계 결과. 각 단계가 성공하면 HTTP 200 으로 내려가고, 응답 code 로 다음 단계를 구분한다.
 * <ul>
 *   <li>{@link Completed}              : 로그인 완료(SUCCESS). accessToken 은 Access-Token 응답 헤더로 내려간다</li>
 *   <li>{@link MfaRequired}            : id/pw 통과, OTP 필요(MFA_REQUIRED)</li>
 *   <li>{@link PasswordChangeRequired} : 임시비밀번호라 새 비밀번호 등록 필요(PASSWORD_CHANGE_REQUIRED)</li>
 * </ul>
 */
public sealed interface LoginStep {

	record Completed(SessionResult session, String accessToken) implements LoginStep {
	}

	record MfaRequired(MfaChallenge challenge) implements LoginStep {
	}

	record PasswordChangeRequired(PasswordChangeChallenge challenge) implements LoginStep {
	}
}
