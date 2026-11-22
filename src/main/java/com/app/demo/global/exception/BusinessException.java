package com.app.demo.global.exception;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * 비즈니스 오류. 검증에 실패한 지점에서 바로 던지고(fail-fast), {@link GlobalExceptionHandler} 가
 * {@link ErrorCode} 의 HTTP status + 공통 봉투(code/message/data)로 변환한다.
 */
@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;
	/** 앱에 함께 내려줄 부가 정보(남은 시도 횟수, 챌린지 토큰 등). 없으면 응답에서 제외된다. */
	private final @Nullable Object data;

	public BusinessException(ErrorCode errorCode) {
		this(errorCode, null);
	}

	public BusinessException(ErrorCode errorCode, @Nullable Object data) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.data = data;
	}
}
