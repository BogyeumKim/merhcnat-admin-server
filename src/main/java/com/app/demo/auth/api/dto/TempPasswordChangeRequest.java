package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.TempPasswordChangeCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 비밀번호 변경 단계 요청(임시비밀번호·만료된 비밀번호): 챌린지 토큰 + 현재 비밀번호 + 새 비밀번호 */
public record TempPasswordChangeRequest(
		@NotBlank @Size(max = 2048) String challengeToken,
		@NotBlank @Size(max = 64) String currentPassword,
		@NotBlank @Size(max = 64) String newPassword) {

	public TempPasswordChangeCommand toCommand() {
		return new TempPasswordChangeCommand(challengeToken, currentPassword, newPassword);
	}
}
