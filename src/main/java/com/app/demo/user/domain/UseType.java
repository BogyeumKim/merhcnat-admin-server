package com.app.demo.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/** 이용 상태 (user.use_type). */
@Getter
@RequiredArgsConstructor
public enum UseType {
	/** 0: 사용 */
	ACTIVE(0),
	/** 1: 정지 */
	SUSPENDED(1),
	/** 2: 삭제 */
	DELETED(2);

	private final int code;

	/** DB 코드 → 상태. NULL 이나 정의되지 않은 값은 null 이고, 로그인에서 막는다. */
	public static @Nullable UseType fromCode(@Nullable Integer code) {
		for (UseType t : values()) {
			if (code != null && t.code == code) {
				return t;
			}
		}
		return null;
	}
}
