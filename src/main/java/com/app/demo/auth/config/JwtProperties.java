package com.app.demo.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** JWT 설정 (prefix: jwt). 값이 잘못되면 서버가 기동되지 않는다. 키 값은 application-local.yaml(git 제외)에 둔다. */
@Getter
@AllArgsConstructor
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

	/** Access-Token HS256 서명 키 (32바이트 이상) */
	@NotBlank
	@Size(min = 32)
	private final String secret;

	/** 챌린지 토큰(mfa·pwc) 전용 HS256 서명 키. Access-Token 키와 분리해 한쪽이 유출돼도 다른 쪽을 위조할 수 없게 한다 */
	@NotBlank
	@Size(min = 32)
	private final String challengeSecret;

	/** Access-Token 유효시간(분) */
	@Positive
	private final long expirationMinutes;
}
