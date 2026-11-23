package com.app.demo.user.domain;

import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/** 사용자 인증 유형 (user.login_type). 기존 MMS 의 1(이메일)·2(SMS)는 등록된 연락처로 수단을 고르는 MESSAGE 하나로 받는다. */
@Getter
@RequiredArgsConstructor
public enum LoginType {
	/** 0: TOTP(인증앱). 기기와 상관없이 매번 OTP */
	TOTP(0),
	/** 1: 카카오/SMS/이메일. 등록된 기기에서는 OTP 생략 */
	MESSAGE(1);

	/** 기존 MMS 의 SMS. 이메일(1)과 함께 MESSAGE 로 받는다. */
	private static final int LEGACY_SMS_CODE = 2;

	private final int code;

	/** DB 코드 → 유형. 정의되지 않은 값/NULL 이면 empty (미지원 유형). ex) 1·2 → MESSAGE */
	public static Optional<LoginType> fromCode(@Nullable Integer code) {
		if (code == null) {
			return Optional.empty();
		}
		if (code == LEGACY_SMS_CODE) {
			return Optional.of(MESSAGE);
		}
		for (LoginType t : values()) {
			if (t.code == code) {
				return Optional.of(t);
			}
		}
		return Optional.empty();
	}
}
