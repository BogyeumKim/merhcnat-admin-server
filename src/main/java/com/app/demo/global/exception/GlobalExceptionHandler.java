package com.app.demo.global.exception;

import com.app.demo.global.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 모든 컨트롤러 예외를 공통 응답 봉투로 변환한다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** 도메인/인증 오류 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException e) {
		return ResponseEntity.status(e.status()).body(new ApiResponse<>(e.code(), e.getMessage(), e.data()));
	}

	/** JSON 형식 오류 등 읽을 수 없는 요청 본문 → 400 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Object>> handleBadBody(HttpMessageNotReadableException e) {
		return ResponseEntity.badRequest().body(new ApiResponse<>("BAD_REQUEST", "요청 형식이 올바르지 않습니다.", null));
	}

	/** 예상하지 못한 오류 → 500 (내부 내용은 노출하지 않고 로그만 남김) */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception e) {
		log.error("Unhandled exception", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiResponse<>("INTERNAL_ERROR", "서버 오류가 발생했습니다.", null));
	}
}
