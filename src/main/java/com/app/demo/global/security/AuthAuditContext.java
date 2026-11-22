package com.app.demo.global.security;

import org.jspecify.annotations.Nullable;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * 인증 감사 로그({@link AuthAuditFilter})에 남길 값을 요청 속성에 모아 두는 곳.
 * 요청을 처리하는 쪽(로그인 서비스·챌린지 검증·JWT 필터·예외 처리기)은 아는 값만 기록하고, 로그는 필터가 요청 끝에 한 줄로 남긴다.
 * 원문 아이디·기기 ID 는 저장하지 않는다(마스킹·해시 앞자리만).
 */
public final class AuthAuditContext {

	static final String USER = AuthAuditContext.class.getName() + ".user";
	static final String DEVICE = AuthAuditContext.class.getName() + ".device";
	static final String RESULT = AuthAuditContext.class.getName() + ".result";

	private static final int DEVICE_HASH_PREFIX = 8;

	private AuthAuditContext() {
	}

	/** 요청 주체: 마스킹한 아이디와 기기 ID 해시 앞 8자 */
	public static void subject(String maskedUserId, @Nullable String deviceIdHash) {
		put(USER, maskedUserId);
		if (deviceIdHash != null) {
			put(DEVICE, deviceIdHash.substring(0, Math.min(DEVICE_HASH_PREFIX, deviceIdHash.length())));
		}
	}

	/** 결과 code (SUCCESS / MFA_REQUIRED / PASSWORD_CHANGE_REQUIRED / 오류 code) */
	public static void result(String code) {
		put(RESULT, code);
	}

	private static void put(String name, String value) {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if (attributes != null) {
			attributes.setAttribute(name, value, RequestAttributes.SCOPE_REQUEST);
		}
	}
}
