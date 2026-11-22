package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.PasswordChangeChallenge;

/**
 * 비밀번호 변경 필요(200, code=PASSWORD_CHANGE_REQUIRED) data. 아직 로그인 전이라 Access-Token 은 없다.
 *
 * @param reason TEMP_PASSWORD(임시비밀번호) | PASSWORD_EXPIRED(비밀번호 만료). 둘 다 해당하면 TEMP_PASSWORD
 */
public record PasswordChangeRequiredResponse(String userId, String challengeToken, String reason) implements AuthStepResponse {

	public static PasswordChangeRequiredResponse from(PasswordChangeChallenge c) {
		return new PasswordChangeRequiredResponse(c.userId(), c.challengeToken(), c.reason().name());
	}
}
