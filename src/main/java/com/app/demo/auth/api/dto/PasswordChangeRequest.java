package com.app.demo.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/** 비밀번호 변경 요청 (현재 비밀번호 필수) */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class PasswordChangeRequest {

	@NotBlank
	@Size(max = 64)
	private final String currentPassword;

	@NotBlank
	@Size(max = 64)
	private final String newPassword;
}
