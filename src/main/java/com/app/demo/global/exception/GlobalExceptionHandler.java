package com.app.demo.global.exception;

import com.app.demo.global.api.ApiResponse;
import com.app.demo.global.api.ErrorData;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 모든 예외를 공통 응답 봉투({@code code, message, data})로 변환하는 단일 지점.
 *
 * <p>예외 처리 정책
 * <ul>
 *   <li>비즈니스 흐름(서비스·컨트롤러)에서는 try-catch 를 쓰지 않는다. 검증에 실패한 곳에서 {@link BusinessException} 을 던지고 여기서 응답을 만든다.</li>
 *   <li>try-catch 는 인프라 경계에서만 쓴다(JWT 파싱 실패 → Optional.empty, DB 중복키 경합, 암호화 라이브러리 checked 예외 → IllegalStateException).</li>
 *   <li>Spring MVC 표준 예외(404, 405, 415, 본문 형식 오류, 파라미터 누락·타입 불일치 등)는 {@link ResponseEntityExceptionHandler} 가 받아
 *       {@link #handleExceptionInternal} 에서 같은 봉투로 바꾼다.</li>
 *   <li>필터 단계의 인증/인가 실패는 컨트롤러에 도달하지 않으므로 global.security 의 {@code SecurityErrorHandler} 가 같은 봉투로 응답한다.</li>
 * </ul>
 * 로그: 클라이언트 오류(4xx)는 WARN 한 줄, 예상하지 못한 오류(5xx)는 ERROR + 스택트레이스. 비밀번호·토큰·OTP 값은 남기지 않는다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	/** 도메인/인증 오류: ErrorCode 의 status·code·message 그대로 응답한다. */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException e, HttpServletRequest request) {
		ErrorCode errorCode = e.getErrorCode();
		log.warn("[{}] {} {} - {}", errorCode.getCode(), request.getMethod(), request.getRequestURI(), errorCode.getMessage());
		return respond(errorCode, e.getData());
	}

	/** 메서드 보안 등 컨트롤러 안에서 발생한 인가 실패 → 403 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
		log.warn("[{}] {} {}", CommonErrorCode.FORBIDDEN.getCode(), request.getMethod(), request.getRequestURI());
		return respond(CommonErrorCode.FORBIDDEN, null);
	}

	/** 예상하지 못한 오류 → 500. 내부 내용은 응답에 노출하지 않고 로그로만 남긴다. */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception e, HttpServletRequest request) {
		log.error("[{}] {} {}", CommonErrorCode.INTERNAL_ERROR.getCode(), request.getMethod(), request.getRequestURI(), e);
		return respond(CommonErrorCode.INTERNAL_ERROR, null);
	}

	/** 요청 본문({@code @Valid @RequestBody}) 검증 실패 → 400 + 필드별 사유 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		List<ErrorData.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(f -> new ErrorData.FieldError(f.getField(), reason(f)))
				.toList();
		return toResponse(ex, CommonErrorCode.INVALID_INPUT, ErrorData.fieldErrors(errors), request);
	}

	/** 메서드 파라미터({@code @PathVariable}, {@code @RequestParam} 등) 검증 실패 → 400 + 필드별 사유 */
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ErrorData.FieldError> errors = ex.getParameterValidationResults().stream()
				.flatMap(r -> r.getResolvableErrors().stream()
						.map(err -> new ErrorData.FieldError(String.valueOf(r.getMethodParameter().getParameterName()), reason(err))))
				.toList();
		return toResponse(ex, CommonErrorCode.INVALID_INPUT, ErrorData.fieldErrors(errors), request);
	}

	/** JSON 문법 오류·타입이 맞지 않는 본문 → 400 */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		return toResponse(ex, CommonErrorCode.MALFORMED_REQUEST, null, request);
	}

	/** 그 밖의 Spring MVC 표준 예외(404, 405, 406, 413, 415, 파라미터 누락·타입 불일치 등): HTTP status 로 ErrorCode 를 고른다. */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		return toResponse(ex, errorCodeOf(statusCode), null, request);
	}

	private static ErrorCode errorCodeOf(HttpStatusCode status) {
		return switch (HttpStatus.valueOf(status.value())) {
			case NOT_FOUND -> CommonErrorCode.NOT_FOUND;
			case METHOD_NOT_ALLOWED -> CommonErrorCode.METHOD_NOT_ALLOWED;
			case NOT_ACCEPTABLE -> CommonErrorCode.NOT_ACCEPTABLE;
			case UNSUPPORTED_MEDIA_TYPE -> CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
			case CONTENT_TOO_LARGE -> CommonErrorCode.PAYLOAD_TOO_LARGE;
			default -> status.is4xxClientError() ? CommonErrorCode.INVALID_INPUT : CommonErrorCode.INTERNAL_ERROR;
		};
	}

	/** Spring MVC 예외용: 로그를 남기고 봉투로 감싼다. */
	private static ResponseEntity<Object> toResponse(Exception ex, ErrorCode errorCode, @Nullable Object data, WebRequest request) {
		String target = request instanceof ServletWebRequest web
				? web.getRequest().getMethod() + " " + web.getRequest().getRequestURI()
				: request.getDescription(false);
		if (errorCode.getStatus().is5xxServerError()) {
			log.error("[{}] {}", errorCode.getCode(), target, ex);
		} else {
			log.warn("[{}] {} - {}", errorCode.getCode(), target, ex.getClass().getSimpleName());
		}
		return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.error(errorCode, data));
	}

	private static ResponseEntity<ApiResponse<Object>> respond(ErrorCode errorCode, @Nullable Object data) {
		return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.error(errorCode, data));
	}

	private static String reason(MessageSourceResolvable error) {
		String message = error.getDefaultMessage();
		return message == null ? "invalid" : message;
	}
}
