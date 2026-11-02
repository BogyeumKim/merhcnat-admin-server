package com.app.demo.auth.config;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** OTP 정책 설정 (prefix: otp). 시간 단위는 초. */
@ConfigurationProperties(prefix = "otp")
public record OtpProperties(
		/** SMS/카카오 OTP 자릿수 */
		int length,
		/** login(1단계) 후 MFA 를 끝내야 하는 제한시간 */
		long challengeTtlSeconds,
		/** OTP 코드 유효시간 */
		long ttlSeconds,
		/** OTP 재발송 최소 간격 */
		long resendSeconds,
		/** OTP HMAC 키 */
		String hashSecret,
		/** 개발용 고정 OTP. 값이 있으면 SMS/카카오 발송 코드와 TOTP 통과 코드가 모두 이 값이 된다. 운영에서는 반드시 비울 것 */
		@Nullable String devFixedCode) {
}
