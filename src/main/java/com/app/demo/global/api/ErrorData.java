package com.app.demo.global.api;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * 오류 응답 data 의 확장 필드. 해당하는 값만 채운다.
 *
 * @param remainingAttempts 잠금까지 남은 시도 횟수
 * @param violations        위반한 규칙 코드 목록(비밀번호 규칙)
 * @param userIdMasked      잠긴 계정 아이디(마스킹)
 * @param lockedAt          잠긴 시각(ISO-8601 + 오프셋). 기록이 없으면 null
 * @param fieldErrors       요청 값 검증 실패 항목(필드명 + 사유)
 */
public record ErrorData(@Nullable Integer remainingAttempts, @Nullable List<String> violations,
		@Nullable String userIdMasked, @Nullable String lockedAt,
		@Nullable List<FieldError> fieldErrors) {

	/** 요청 값 검증 실패 항목 */
	public record FieldError(String field, String reason) {
	}

	public static ErrorData remaining(int remainingAttempts) {
		return new ErrorData(remainingAttempts, null, null, null, null);
	}

	public static ErrorData violations(List<String> violations) {
		return new ErrorData(null, violations, null, null, null);
	}

	public static ErrorData locked(String userIdMasked, @Nullable String lockedAt) {
		return new ErrorData(null, null, userIdMasked, lockedAt, null);
	}

	public static ErrorData fieldErrors(List<FieldError> fieldErrors) {
		return new ErrorData(null, null, null, null, fieldErrors);
	}
}
