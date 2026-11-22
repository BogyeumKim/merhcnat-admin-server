package com.app.demo.user.domain;

/** 로그인을 완료하기 전에 새 비밀번호 등록이 필요한 이유. 둘 다 해당하면 TEMP_PASSWORD 가 우선한다. */
public enum PasswordChangeReason {
	/** 발급받은 임시비밀번호 */
	TEMP_PASSWORD,
	/** 마지막 변경 후 만료 일수 경과 */
	PASSWORD_EXPIRED
}
