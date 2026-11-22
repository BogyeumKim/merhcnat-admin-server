package com.app.demo.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 설정 (prefix: jwt). 값이 잘못되면 서버가 기동되지 않는다.
 *
 * @param 32 HS256 서명 키 (32바이트 이상)
 * @param expirationMinutes Access-Token 유효시간(분)
 */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
		@NotBlank @Size(min = 32) String secret,
		@Positive long expirationMinutes) {
}
