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
 * @param challengeTtlSeconds login(1단계) 후 MFA 를 끝내야 하는 기본 제한시간. OTP 를 발송할 때마다 (OTP 만료 + 60초)까지 연장된다
 * @param challengeMaxSeconds MFA 챌린지의 절대 상한(로그인 시점 기준). 발송으로 연장해도 이 시간을 넘지 않는다
 * @param ttlSeconds OTP 코드 유효시간
 * @param resendSeconds 같은 수단으로 OTP 를 재발송할 수 있는 최소 간격 (다른 수단은 즉시 발송 가능)
 * @param maxSendsPerChallenge MFA 챌린지 1개(로그인 1회)당 최대 발송 횟수
 * @param maxSendsPerHour 사용자당 1시간 동안 최대 발송 횟수
 * @param hashSecret OTP HMAC 키 (application-local.yaml)
 * @param devFixedCode 개발용 고정 OTP. 값이 있으면 SMS/카카오 발송 코드와 TOTP 통과 코드가 모두 이 값이 된다. local 프로필이 아니면 값이 있을 때 기동을 막는다
 */
@Validated
@ConfigurationProperties(prefix = "otp")
public record OtpProperties(
		@Min(4) @Max(10) int length,
		@Positive long challengeTtlSeconds,
		@Positive long challengeMaxSeconds,
		@Positive long ttlSeconds,
		@Positive long resendSeconds,
		@Positive int maxSendsPerChallenge,
		@Positive int maxSendsPerHour,
		@NotBlank String hashSecret,
		@Nullable String devFixedCode) {
}
