package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.TempPasswordChangeCommand;
import jakarta.validation.constraints.NotBlank;

/** 임시비밀번호 변경 요청: 챌린지 토큰 + 현재(임시) 비밀번호 + 새 비밀번호 */
public record TempPasswordChangeRequest(
		@NotBlank String challengeToken,
		@NotBlank String currentPassword,
		@NotBlank String newPassword) {

	public TempPasswordChangeCommand toCommand() {
		return new TempPasswordChangeCommand(challengeToken, currentPassword, newPassword);
	}
}
