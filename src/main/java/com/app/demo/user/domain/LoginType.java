package com.app.demo.user.domain;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** 사용자 인증 유형 (user.login_type). */
public enum LoginType {
	/** 0: TOTP(인증앱). 기기와 상관없이 매번 OTP */
	TOTP(0),
	/** 1: 카카오/SMS. 등록된 기기에서는 OTP 생략 */
	MESSAGE(1);

	private final int code;

	LoginType(int code) {
		this.code = code;
	}

	public int code() {
		return code;
	}

	/** DB 코드 → 유형. 정의되지 않은 값/NULL 이면 empty (미지원 유형) */
	public static Optional<LoginType> fromCode(@Nullable Integer code) {
		if (code == null) {
			return Optional.empty();
		}
		for (LoginType t : values()) {
			if (t.code == code) {
				return Optional.of(t);
			}
		}
		return Optional.empty();
	}
}
