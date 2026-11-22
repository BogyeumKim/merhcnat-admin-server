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

	/** DB 코드 → 상태. NULL 이나 정의되지 않은 값은 사용(ACTIVE)으로 본다. */
	public static UseType fromCode(@Nullable Integer code) {
		for (UseType t : values()) {
			if (code != null && t.code == code) {
				return t;
			}
		}
		return ACTIVE;
	}
}
