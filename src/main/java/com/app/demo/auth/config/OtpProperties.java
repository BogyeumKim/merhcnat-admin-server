package com.app.demo.auth.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** OTP 정책 설정 (prefix: otp). 시간 단위는 초. 값이 잘못되면 서버가 기동되지 않는다. */
@Getter
@AllArgsConstructor
@Validated
@ConfigurationProperties(prefix = "otp")
public class OtpProperties {

	/** SMS/카카오 OTP 자릿수 */
	@Min(4)
	@Max(10)
	private final int length;

	/** login(1단계) 후 MFA 를 끝내야 하는 기본 제한시간. OTP 를 발송할 때마다 (OTP 만료 + 60초)까지 연장된다 */
	@Positive
	private final long challengeTtlSeconds;

	/** MFA 챌린지의 절대 상한(로그인 시점 기준). 발송으로 연장해도 이 시간을 넘지 않는다 */
	@Positive
	private final long challengeMaxSeconds;

	/** OTP 코드 유효시간 */
	@Positive
	private final long ttlSeconds;

	/** 앱의 "N초 후 다시 받기" 표시용 힌트(응답의 resendAfterSeconds). 서버는 이 값으로 재발송을 막지 않는다 */
	@Positive
	private final long resendHintSeconds;

	/** MFA 챌린지 1개(로그인 1회)당 최대 발송 횟수(수단 합산). 넘으면 처음부터 다시 로그인 */
	@Positive
	private final int maxSendsPerChallenge;

	/** OTP HMAC 키 (application-local.yaml) */
	@NotBlank
	private final String hashSecret;

	/** 개발용 고정 OTP. 값이 있으면 SMS/카카오 발송 코드와 TOTP 통과 코드가 모두 이 값이 된다. local 프로필이 아니면 값이 있을 때 기동을 막는다 */
	private final @Nullable String devFixedCode;
}
