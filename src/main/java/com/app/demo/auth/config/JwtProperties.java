package com.app.demo.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** JWT 설정 (prefix: jwt). */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
		/** HS256 서명 키 (32바이트 이상) */
		String secret,
		/** Access-Token 유효시간(분) */
		long expirationMinutes) {
}
