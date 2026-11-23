package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.TempPasswordChangeCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/** 비밀번호 변경 단계 요청(임시비밀번호·만료된 비밀번호): 챌린지 토큰 + 현재 비밀번호 + 새 비밀번호 */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class TempPasswordChangeRequest {

	@NotBlank
	@Size(max = 2048)
	private final String challengeToken;

	@NotBlank
	@Size(max = 64)
	private final String currentPassword;

	@NotBlank
	@Size(max = 64)
	private final String newPassword;

	public TempPasswordChangeCommand toCommand() {
		return TempPasswordChangeCommand.with()
				.challengeToken(challengeToken)
				.currentPassword(currentPassword)
				.newPassword(newPassword)
				.create();
	}
}
