package com.app.demo.auth.api;

import com.app.demo.global.api.ApiResponse;
import org.springframework.http.ResponseEntity;

/** 새 Access-Token 을 내려주는 200 응답 (로그인 완료, 비밀번호 변경으로 세션 교체). 헤더 값은 Bearer 없이 JWT 원문이다. */
final class AccessTokenHeader {

	static final String NAME = "Access-Token";

	private AccessTokenHeader() {
	}

	static <T> ResponseEntity<ApiResponse<T>> ok(String accessToken, ApiResponse<T> body) {
		return ResponseEntity.ok().header(NAME, accessToken).body(body);
	}
}
