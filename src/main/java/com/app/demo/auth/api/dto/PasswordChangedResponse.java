package com.app.demo.auth.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 상태 비밀번호 변경 성공(200, code=SUCCESS) data.
 * 세션이 새 토큰으로 교체되므로 앱은 이후 요청에 이 accessToken 을 쓴다(이전 토큰은 즉시 SESSION_REPLACED).
 */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordChangedResponse {

	/** 새 Access-Token(JWT) */
	private final String accessToken;
}
