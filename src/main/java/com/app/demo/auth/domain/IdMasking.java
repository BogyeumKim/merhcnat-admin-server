package com.app.demo.auth.domain;

/** 아이디 마스킹: 6자 이상은 앞 3자 + 별표 + 뒤 2자, 짧은 아이디는 앞 1자 + 별표 + (4자 이상이면) 뒤 1자. 원문은 내려주지 않는다. */
public final class IdMasking {

	private IdMasking() {
	}

	public static String mask(String userId) {
		int n = userId.length();
		if (n >= 6) {
			return userId.substring(0, 3) + "*".repeat(n - 5) + userId.substring(n - 2);
		}
		if (n >= 4) {
			return userId.charAt(0) + "*".repeat(n - 2) + userId.charAt(n - 1);
		}
		return n == 0 ? "" : userId.charAt(0) + "*".repeat(n - 1);
	}
}
