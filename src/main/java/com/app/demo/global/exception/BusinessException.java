package com.app.demo.global.exception;

import com.app.demo.global.api.ErrorDetail;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * 비즈니스 오류. 검증에 실패한 지점에서 바로 던지고(fail-fast), {@link GlobalExceptionHandler} 가
 * {@link ErrorCode} 의 HTTP status + 실패 응답 ErrorResponse(code/message/data)로 변환한다.
 */
@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;
	/** 앱에 함께 내려줄 부가 정보(남은 시도 횟수, 잠금 시각 등). 없으면 응답에서 제외된다. */
	private final @Nullable ErrorDetail data;

	public BusinessException(ErrorCode errorCode) {
		this(errorCode, null);
	}

	public BusinessException(ErrorCode errorCode, @Nullable ErrorDetail data) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.data = data;
	}
}
