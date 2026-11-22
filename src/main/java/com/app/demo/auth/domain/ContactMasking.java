package com.app.demo.auth.domain;

/**
 * 연락처 마스킹. 원문은 응답·로그 어디에도 내보내지 않는다.
 * <ul>
 *   <li>휴대폰: 숫자만 남겨 앞 3자리 + "-****-" + 뒤 4자리 (예: 010-****-1234). 7자리 미만이면 "****" + 뒤 최대 4자리</li>
 *   <li>이메일: 로컬파트 앞 2자 + "****" + "@도메인" (로컬파트 2자 이하면 앞 1자). 형식이 아니면 "****"</li>
 * </ul>
 */
public final class ContactMasking {

	private static final String MASK = "****";

	private ContactMasking() {
	}

	/** 수단에 맞게 마스킹한다. */
	public static String mask(OtpChannel channel, String contact) {
		return channel.usesPhone() ? maskPhone(contact) : maskEmail(contact);
	}

	public static String maskPhone(String phone) {
		String digits = phone.replaceAll("\\D", "");
		if (digits.length() < 7) {
			return MASK + digits.substring(Math.max(0, digits.length() - 4));
		}
		return digits.substring(0, 3) + "-" + MASK + "-" + digits.substring(digits.length() - 4);
	}

	public static String maskEmail(String email) {
		String trimmed = email.trim();
		int at = trimmed.lastIndexOf('@');
		if (at <= 0 || at == trimmed.length() - 1) {
			return MASK;
		}
		String local = trimmed.substring(0, at);
		int visible = local.length() <= 2 ? 1 : 2;
		return local.substring(0, visible) + MASK + trimmed.substring(at);
	}
}
