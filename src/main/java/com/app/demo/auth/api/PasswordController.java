package com.app.demo.auth.api;

import com.app.demo.auth.api.dto.PasswordChangeRequest;
import com.app.demo.auth.application.PasswordService;
import com.app.demo.global.api.ApiResponse;
import com.app.demo.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 비밀번호 API (Bearer Access-Token 필요) */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class PasswordController {

	private final PasswordService passwordService;

	/** 비밀번호 변경. 성공 시 200, 이후 로그인 응답의 passwordChangeRequired 는 false 가 된다. */
	@PutMapping("/password")
	public ApiResponse<Void> change(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody PasswordChangeRequest request) {
		passwordService.change(user, request.currentPassword(), request.newPassword());
		return ApiResponse.ok();
	}
}
