package com.app.demo.global.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/** 오류 응답 data 의 확장 필드. 해당하는 값만 채운다. */
@JsonPropertyOrder({"remainingAttempts", "violations", "userIdMasked", "lockedAt", "fieldErrors"})
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorData implements ErrorDetail {

	/** 잠금까지 남은 시도 횟수 */
	private final @Nullable Integer remainingAttempts;

	/** 위반한 규칙 코드 목록(비밀번호 규칙) */
	private final @Nullable List<String> violations;

	/** 잠긴 계정 아이디(마스킹) */
	private final @Nullable String userIdMasked;

	/** 잠긴 시각(ISO-8601 + 오프셋). 기록이 없으면 null */
	private final @Nullable String lockedAt;

	/** 요청 값 검증 실패 항목(필드명 + 사유) */
	private final @Nullable List<FieldError> fieldErrors;

	/** 요청 값 검증 실패 항목 */
	@JsonPropertyOrder({"field", "reason"})
	@Getter
	@Builder(builderMethodName = "with", buildMethodName = "create")
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class FieldError {

		private final String field;

		private final String reason;
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
