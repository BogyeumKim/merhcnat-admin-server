package com.app.demo.global.exception;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 공격자가 보낸 값(경로·메서드)을 로그에 남길 때 쓰는 정리 도구.
 * 제어 문자(CR/LF 등)는 '?' 로 바꿔 로그 위조(log injection)를 막고, 길이는 잘라 로그 폭주를 막는다.
 */
public final class LogSafe {

	private static final int MAX_LENGTH = 200;

	private LogSafe() {
	}

	/** "METHOD URI" 형태의 로그용 요청 표기 */
	public static String target(HttpServletRequest request) {
		return clean(request.getMethod()) + " " + clean(request.getRequestURI());
	}

	static String clean(String value) {
		String truncated = value.length() > MAX_LENGTH ? value.substring(0, MAX_LENGTH) + "…" : value;
		StringBuilder sb = new StringBuilder(truncated.length());
		truncated.codePoints().forEach(c -> sb.appendCodePoint(Character.isISOControl(c) ? '?' : c));
		return sb.toString();
	}
}
