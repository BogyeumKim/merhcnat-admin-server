package com.app.demo.auth.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 잠금 정책 설정 (prefix: auth). 값이 잘못되면 서버가 기동되지 않는다.
 *
 * @param maxFailCount user.password_fail_count 가 이 값에 도달하면 로그인 차단. id/pw·OTP 오류 합산
 * @param passwordExpireDays 마지막 비밀번호 변경 후 이 일수가 지나면 변경 필수
 * @param sessionIdleMinutes 마지막 요청 후 이 시간 동안 활동이 없으면 로그인 세션 만료 (Access-Token 만료가 절대 상한)
 */
@Validated
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
		@Positive int maxFailCount,
		@Positive int passwordExpireDays,
		@Positive int sessionIdleMinutes) {
}
