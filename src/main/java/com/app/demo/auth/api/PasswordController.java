package com.app.demo.auth.api;

import com.app.demo.auth.api.dto.PasswordChangeRequest;
import com.app.demo.auth.api.dto.PasswordChangedResponse;
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

	/**
	 * 비밀번호 변경. 성공 시 200 + data.accessToken(새 토큰, 세션 교체 → 이전 토큰은 즉시 SESSION_REPLACED). 로그인은 유지된다.
	 * 400 CURRENT_PASSWORD_INVALID / PASSWORD_POLICY_VIOLATION, 409 PASSWORD_REUSED, 423 ACCOUNT_LOCKED
	 */
	@PutMapping("/password")
	public ApiResponse<PasswordChangedResponse> change(@AuthenticationPrincipal AuthUser user,
			@Valid @RequestBody PasswordChangeRequest request) {
		return ApiResponse.ok(new PasswordChangedResponse(
				passwordService.change(user, request.currentPassword(), request.newPassword()).value()));
	}
}
