package com.app.demo.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 비밀번호 변경 요청 (현재 비밀번호 필수) */
public record PasswordChangeRequest(@NotBlank @Size(max = 64) String currentPassword, @NotBlank @Size(max = 64) String newPassword) {
}
