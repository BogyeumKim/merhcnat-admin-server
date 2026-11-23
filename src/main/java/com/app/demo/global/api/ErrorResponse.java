package com.app.demo.global.api;

import com.app.demo.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/** 실패 응답 본문. HTTP 상태는 {@link ErrorCode} 가 정하고, 앱은 {@code code} 로 분기한다. 성공 응답은 각 API 의 DTO 를 그대로 쓴다. */
@JsonPropertyOrder({"code", "message", "data"})
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse {

	/** 앱이 분기하는 기계용 코드 */
	private final String code;

	/** 사람이 읽는 참고 문구 */
	private final String message;

	/** 오류 부가 정보(남은 횟수·점검 시각 등). 없으면 응답에서 빠진다 */
	private final @Nullable ErrorDetail data;

	public static ErrorResponse of(ErrorCode errorCode) {
		return of(errorCode, errorCode.getMessage(), null);
	}

	public static ErrorResponse of(ErrorCode errorCode, @Nullable ErrorDetail data) {
		return of(errorCode, errorCode.getMessage(), data);
	}

	/** 상황별 문구를 쓸 때 */
	public static ErrorResponse of(ErrorCode errorCode, String message, @Nullable ErrorDetail data) {
		return new ErrorResponse(errorCode.getCode(), message, data);
	}
}
