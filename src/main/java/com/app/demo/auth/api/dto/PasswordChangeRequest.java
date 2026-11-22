package com.app.demo.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

/** 비밀번호 변경 요청 (현재 비밀번호 필수) */
public record PasswordChangeRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
}
