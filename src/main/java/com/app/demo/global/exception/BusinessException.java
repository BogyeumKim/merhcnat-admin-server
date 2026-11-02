package com.app.demo.global.exception;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

/** 비즈니스 오류. HTTP status + 공통 봉투의 code/message/data 로 변환된다. */
public class BusinessException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final @Nullable Object data;

	public BusinessException(HttpStatus status, String code, String message) {
		this(status, code, message, null);
	}

	public BusinessException(HttpStatus status, String code, String message, @Nullable Object data) {
		super(message);
		this.status = status;
		this.code = code;
		this.data = data;
	}

	/** 생성자에서 항상 메시지를 받으므로 null 이 아니다 (부모 시그니처는 @Nullable). */
	@Override
	public String getMessage() {
		String m = super.getMessage();
		return m == null ? "" : m;
	}

	public HttpStatus status() {
		return status;
	}

	public String code() {
		return code;
	}

	public @Nullable Object data() {
		return data;
	}
}
