package com.app.demo.auth.domain;

import java.util.OptionalLong;

/** TOTP(RFC 6238) 검증 포트. */
public interface TotpVerifier {

	/**
	 * 코드를 검증한다(시계 오차 ±1 step 허용).
	 *
	 * @return 일치한 time-step (재사용 방지에 사용), 불일치면 empty
	 */
	OptionalLong verify(String base32Secret, String code);
}
