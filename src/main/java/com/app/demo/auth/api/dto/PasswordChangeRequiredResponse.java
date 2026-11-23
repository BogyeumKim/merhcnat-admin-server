package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.PasswordChangeChallenge;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 비밀번호 변경 필요(200, code=PASSWORD_CHANGE_REQUIRED) data. 아직 로그인 전이라 Access-Token 은 없다. */
@JsonPropertyOrder({"userId", "challengeToken", "reason"})
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PasswordChangeRequiredResponse implements AuthStepResponse {

	private final String userId;

	private final String challengeToken;

	/** TEMP_PASSWORD(임시비밀번호) | PASSWORD_EXPIRED(비밀번호 만료). 둘 다 해당하면 TEMP_PASSWORD */
	private final String reason;

	public static PasswordChangeRequiredResponse from(PasswordChangeChallenge c) {
		String reason = c.getReason().name();
		return PasswordChangeRequiredResponse.with()
				.userId(c.getUserId())
				.challengeToken(c.getChallengeToken())
				.reason(reason)
				.create();
	}
}
