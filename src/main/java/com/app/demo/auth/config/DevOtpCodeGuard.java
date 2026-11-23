package com.app.demo.auth.config;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * 개발용 고정 OTP(otp.dev-fixed-code)가 local 프로필 밖에서 켜져 있으면 서버 기동을 막는다.
 * 고정 OTP 가 운영에 남으면 누구나 2단계 인증을 통과하므로, 설정 실수를 기동 시점에 바로 드러낸다.
 */
@Component
class DevOtpCodeGuard {

	DevOtpCodeGuard(OtpProperties otpProps, Environment environment) {
		String fixed = otpProps.getDevFixedCode();
		boolean enabled = fixed != null && !fixed.isBlank();
		if (enabled && !environment.acceptsProfiles(Profiles.of("local"))) {
			throw new IllegalStateException("otp.dev-fixed-code 는 local 프로필에서만 사용할 수 있습니다. 운영 설정에서 제거하세요.");
		}
	}
}
