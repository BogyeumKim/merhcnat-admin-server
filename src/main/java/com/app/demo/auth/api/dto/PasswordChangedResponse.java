package com.app.demo.auth.api.dto;

/**
 * 로그인 상태 비밀번호 변경 성공(200, code=SUCCESS) data.
 * 세션이 새 토큰으로 교체되므로 앱은 이후 요청에 이 accessToken 을 쓴다(이전 토큰은 즉시 SESSION_REPLACED).
 *
 * @param accessToken 새 Access-Token(JWT)
 */
public record PasswordChangedResponse(String accessToken) {
}
