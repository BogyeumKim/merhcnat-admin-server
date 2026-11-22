package com.app.demo.auth.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 비밀번호 규칙. 재사용 금지는 이력이 필요해 {@code User#passwordReused} 에서 검사한다.
 * <ul>
 *   <li>LENGTH: 8~20자</li>
 *   <li>CHARSET: 영문 [A-Za-z]·숫자 [0-9]·특수문자(공백을 뺀 출력 가능한 ASCII 기호)를 모두 포함하고,
 *       그 밖의 문자(공백·한글 등 비ASCII·전각 문자·이모지·제어문자)는 하나도 없어야 한다</li>
 *   <li>CONTAINS_USER_ID: 아이디 문자열 포함 불가(대소문자 무시)</li>
 * </ul>
 */
public final class PasswordPolicy {

	public static final String LENGTH = "LENGTH";
	public static final String CHARSET = "CHARSET";
	public static final String CONTAINS_USER_ID = "CONTAINS_USER_ID";

	private static final int MIN_LENGTH = 8;
	private static final int MAX_LENGTH = 20;

	private static final Pattern LETTER = Pattern.compile("[A-Za-z]");
	private static final Pattern DIGIT = Pattern.compile("[0-9]");
	/** 공백을 뺀 출력 가능한 ASCII 기호: !"#$%&'()*+,-./ :;<=>?@ [\]^_` {|}~ */
	private static final Pattern SPECIAL = Pattern.compile("[!-/:-@\\[-`{-~]");
	/** 허용 문자만으로 이뤄졌는지 */
	private static final Pattern ALLOWED_ONLY = Pattern.compile("^[A-Za-z0-9!-/:-@\\[-`{-~]+$");

	private PasswordPolicy() {
	}

	/** 위반 코드 목록 (LENGTH → CHARSET → CONTAINS_USER_ID 순). 비어 있으면 통과 */
	public static List<String> violations(String userId, String password) {
		List<String> violations = new ArrayList<>();
		if (!hasValidLength(password)) {
			violations.add(LENGTH);
		}
		if (!hasValidCharset(password)) {
			violations.add(CHARSET);
		}
		if (containsUserId(userId, password)) {
			violations.add(CONTAINS_USER_ID);
		}
		return List.copyOf(violations);
	}

	private static boolean hasValidLength(String password) {
		return password.length() >= MIN_LENGTH && password.length() <= MAX_LENGTH;
	}

	/** 영문·숫자·특수문자를 하나 이상씩 포함하고, 허용되지 않은 문자가 없어야 한다. */
	private static boolean hasValidCharset(String password) {
		return LETTER.matcher(password).find()
				&& DIGIT.matcher(password).find()
				&& SPECIAL.matcher(password).find()
				&& ALLOWED_ONLY.matcher(password).matches();
	}

	private static boolean containsUserId(String userId, String password) {
		return !userId.isBlank() && password.toLowerCase(Locale.ROOT).contains(userId.toLowerCase(Locale.ROOT));
	}
}
