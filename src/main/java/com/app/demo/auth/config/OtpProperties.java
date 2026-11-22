package com.app.demo.auth.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * OTP 정책 설정 (prefix: otp). 시간 단위는 초. 값이 잘못되면 서버가 기동되지 않는다.
 *
 * @param length SMS/카카오 OTP 자릿수
 * @param challengeTtlSeconds login(1단계) 후 MFA 를 끝내야 하는 제한시간
 * @param ttlSeconds OTP 코드 유효시간
 * @param resendSeconds OTP 재발송 최소 간격
 * @param hashSecret OTP HMAC 키
 * @param devFixedCode 개발용 고정 OTP. 값이 있으면 SMS/카카오 발송 코드와 TOTP 통과 코드가 모두 이 값이 된다. 운영에서는 반드시 비울 것
 */
@Validated
@ConfigurationProperties(prefix = "otp")
public record OtpProperties(
		@Min(4) @Max(10) int length,
		@Positive long challengeTtlSeconds,
		@Positive long ttlSeconds,
		@Positive long resendSeconds,
		@NotBlank String hashSecret,
		@Nullable String devFixedCode) {
}
