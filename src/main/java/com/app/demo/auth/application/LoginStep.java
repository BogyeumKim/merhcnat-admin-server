package com.app.demo.auth.application;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 로그인 단계 결과. 각 단계가 성공하면 HTTP 200 으로 내려가고, 응답 code 로 다음 단계를 구분한다.
 * {@link Completed}: 로그인 완료(SUCCESS, accessToken 은 응답 본문) / {@link MfaRequired}: id/pw 통과, OTP 필요(MFA_REQUIRED)
 * / {@link PasswordChangeRequired}: 임시비밀번호라 새 비밀번호 등록 필요(PASSWORD_CHANGE_REQUIRED)
 */
public sealed interface LoginStep {

	@Getter
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	final class Completed implements LoginStep {

		private final SessionResult session;

		private final String accessToken;

		public static Completed of(SessionResult session, String accessToken) {
			return new Completed(session, accessToken);
		}
	}

	@Getter
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	final class MfaRequired implements LoginStep {

		private final MfaChallenge challenge;

		public static MfaRequired of(MfaChallenge challenge) {
			return new MfaRequired(challenge);
		}
	}

	@Getter
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	final class PasswordChangeRequired implements LoginStep {

		private final PasswordChangeChallenge challenge;

		public static PasswordChangeRequired of(PasswordChangeChallenge challenge) {
			return new PasswordChangeRequired(challenge);
		}
	}
}
