package com.app.demo.auth.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 비밀번호 규칙: 8~20자, 영문·숫자·특수문자 모두 포함, 아이디 문자열 포함 불가. (재사용 금지는 이력이 필요해 서비스에서 검사) */
public final class PasswordPolicy {

	public static final String LENGTH = "LENGTH";
	public static final String CHARSET = "CHARSET";
	public static final String CONTAINS_USER_ID = "CONTAINS_USER_ID";

	private PasswordPolicy() {
	}

	/** 위반 항목 코드 목록 (없으면 빈 목록 = 통과) */
	public static List<String> violations(String userId, String password) {
		List<String> v = new ArrayList<>();
		if (password.length() < 8 || password.length() > 20) {
			v.add(LENGTH);
		}
		boolean letter = password.chars().anyMatch(Character::isLetter);
		boolean digit = password.chars().anyMatch(Character::isDigit);
		boolean special = password.chars().anyMatch(c -> !Character.isLetter(c) && !Character.isDigit(c));
		if (!(letter && digit && special)) {
			v.add(CHARSET);
		}
		if (!userId.isBlank() && password.toLowerCase(Locale.ROOT).contains(userId.toLowerCase(Locale.ROOT))) {
			v.add(CONTAINS_USER_ID);
		}
		return v;
	}
}
