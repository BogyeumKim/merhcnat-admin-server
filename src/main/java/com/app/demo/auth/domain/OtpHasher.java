package com.app.demo.auth.domain;

/** OTP 해시/비교 포트. 6자리는 경우의 수가 작아 단순 SHA 는 역산이 쉬우므로 키 기반(HMAC) 해시를 쓴다. */
public interface OtpHasher {

	/** userId 에 바인딩된 해시(hex) */
	String hash(String userId, String code);

	/** 상수 시간 비교 (타이밍 공격 방지) */
	boolean matches(String userId, String code, String storedHash);
}
