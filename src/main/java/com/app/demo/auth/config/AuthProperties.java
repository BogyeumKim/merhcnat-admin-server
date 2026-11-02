package com.app.demo.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 잠금 정책 설정 (prefix: auth). */
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
		/** user.password_fail_count 가 이 값에 도달하면 로그인 차단. id/pw·OTP 오류 합산 */
		int maxFailCount) {
}
