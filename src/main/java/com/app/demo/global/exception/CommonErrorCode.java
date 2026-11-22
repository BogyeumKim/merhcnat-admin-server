package com.app.demo.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 특정 맥락에 속하지 않는 공통 오류 (요청 형식, 인증/인가, 라우팅, 서버 오류). */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

	INVALID_INPUT(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "요청 값이 올바르지 않습니다."),
	MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "요청 형식이 올바르지 않습니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 경로를 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식입니다."),
	NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE", "지원하지 않는 응답 형식입니다."),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 요청 형식입니다."),
	PAYLOAD_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "PAYLOAD_TOO_LARGE", "요청 크기가 너무 큽니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다."),
	SESSION_STORE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SESSION_STORE_UNAVAILABLE", "일시적으로 인증을 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.");

	private final HttpStatus status;
	private final String code;
	private final String message;

	/**
	 * 프레임워크/서버가 정한 HTTP status 에 맞는 공통 오류. Spring MVC 표준 예외, 오류 페이지(/error), Tomcat 단계 오류가 함께 쓴다.
	 * 정의되지 않은 4xx 는 BAD_REQUEST, 5xx 는 INTERNAL_ERROR 로 묶는다.
	 */
	public static CommonErrorCode fromStatus(int status) {
		return switch (status) {
			case 401 -> UNAUTHORIZED;
			case 403 -> FORBIDDEN;
			case 404 -> NOT_FOUND;
			case 405 -> METHOD_NOT_ALLOWED;
			case 406 -> NOT_ACCEPTABLE;
			case 413 -> PAYLOAD_TOO_LARGE;
			case 415 -> UNSUPPORTED_MEDIA_TYPE;
			default -> status >= 400 && status < 500 ? INVALID_INPUT : INTERNAL_ERROR;
		};
	}
}
