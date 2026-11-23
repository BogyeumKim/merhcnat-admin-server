package com.app.demo.auth.api;

import com.app.demo.auth.api.dto.PasswordChangeRequest;
import com.app.demo.auth.api.dto.PasswordChangedResponse;
import com.app.demo.auth.application.PasswordService;
import com.app.demo.auth.domain.AccessToken;
import com.app.demo.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 비밀번호 API (Bearer Access-Token 필요) */
@RestController
@RequiredArgsConstructor
@Slf4j
public class PasswordController {

	private final PasswordService passwordService;

	/**
	 * 비밀번호 변경. 성공 시 200 + accessToken(새 토큰, 세션 교체 → 이전 토큰은 즉시 SESSION_REPLACED). 로그인은 유지된다.
	 * 400 CURRENT_PASSWORD_INVALID / PASSWORD_POLICY_VIOLATION, 409 PASSWORD_REUSED, 423 ACCOUNT_LOCKED
	 */
	@PutMapping(value = "/api/users/me/password", consumes = MediaType.APPLICATION_JSON_VALUE)
	public PasswordChangedResponse change(
			@AuthenticationPrincipal AuthUser user,
			@Valid @RequestBody PasswordChangeRequest request) {

		log.info("Password change received");

		AccessToken token = passwordService.change(user, request.getCurrentPassword(), request.getNewPassword());

		return PasswordChangedResponse.from(token);
	}
}
