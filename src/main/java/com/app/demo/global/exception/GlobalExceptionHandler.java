package com.app.demo.global.exception;

import com.app.demo.global.api.ErrorResponse;
import com.app.demo.global.api.ErrorData;
import com.app.demo.global.api.ErrorDetail;
import com.app.demo.global.security.AuthAuditContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
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
 * 모든 예외를 실패 응답 {@link ErrorResponse}({@code code, message, data})로 변환하는 단일 지점.
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
 * 요청 경로는 공격자가 정하는 값이라 {@link LogSafe} 로 잘라서(제어 문자 제거) 남긴다.
 *
 * <p>오류 응답은 Content-Type 을 JSON 으로 고정한다. 클라이언트가 {@code Accept: application/xml} 처럼 JSON 을 받지 않겠다고 해도
 * 오류 봉투는 항상 JSON 으로 내려간다(그렇지 않으면 오류 응답 자체를 쓰지 못해 상태 코드가 뒤바뀐다).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	/** 도메인/인증 오류: ErrorCode 의 status·code·message 그대로 응답한다. */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest request) {
		ErrorCode errorCode = e.getErrorCode();
		log.warn("[{}] {} - {}", errorCode.getCode(), LogSafe.target(request), errorCode.getMessage());
		return respond(errorCode, e.getData());
	}

	/** 메서드 보안 등 컨트롤러 안에서 발생한 인가 실패 → 403 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
		log.warn("[{}] {}", CommonErrorCode.FORBIDDEN.getCode(), LogSafe.target(request));
		return respond(CommonErrorCode.FORBIDDEN, null);
	}

	/** 예상하지 못한 오류 → 500. 내부 내용은 응답에 노출하지 않고 로그로만 남긴다. */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
		log.error("[{}] {}", CommonErrorCode.INTERNAL_ERROR.getCode(), LogSafe.target(request), e);
		return respond(CommonErrorCode.INTERNAL_ERROR, null);
	}

	/** 요청 본문({@code @Valid @RequestBody}) 검증 실패 → 400 + 필드별 사유 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		List<ErrorData.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(f -> ErrorData.FieldError.with()
						.field(f.getField())
						.reason(reason(f))
						.create())
				.toList();
		return toResponse(ex, CommonErrorCode.INVALID_INPUT, ErrorData.fieldErrors(errors), HttpHeaders.EMPTY, request);
	}

	/** 메서드 파라미터({@code @PathVariable}, {@code @RequestParam} 등) 검증 실패 → 400 + 필드별 사유 */
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ErrorData.FieldError> errors = ex.getParameterValidationResults().stream()
				.flatMap(r -> r.getResolvableErrors().stream()
						.map(err -> ErrorData.FieldError.with()
								.field(String.valueOf(r.getMethodParameter().getParameterName()))
								.reason(reason(err))
								.create()))
				.toList();
		return toResponse(ex, CommonErrorCode.INVALID_INPUT, ErrorData.fieldErrors(errors), HttpHeaders.EMPTY, request);
	}

	/** JSON 문법 오류·타입이 맞지 않는 본문·빈 본문 → 400 */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		return toResponse(ex, CommonErrorCode.MALFORMED_REQUEST, null, HttpHeaders.EMPTY, request);
	}

	/**
	 * 그 밖의 Spring MVC 표준 예외(404, 405, 406, 413, 415, 파라미터 누락·타입 불일치 등): HTTP status 로 ErrorCode 를 고른다.
	 * 프레임워크가 정한 헤더(405 의 Allow, 415 의 Accept 등 HTTP 표준이 요구하는 헤더)는 그대로 내려준다.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		return toResponse(ex, CommonErrorCode.fromStatus(statusCode.value()), null, headers, request);
	}

	/**
	 * Spring MVC 예외용: 로그를 남기고 봉투로 감싼다.
	 * 반환 타입이 {@code ResponseEntity<Object>} 인 것은 부모 {@link ResponseEntityExceptionHandler} 의 재정의 메서드 시그니처를
	 * 따라야 하기 때문이며, 본문은 항상 {@link ErrorResponse} 이다.
	 */
	private static ResponseEntity<Object> toResponse(Exception ex, ErrorCode errorCode, @Nullable ErrorDetail data, HttpHeaders headers,
			WebRequest request) {
		String target = request instanceof ServletWebRequest web
				? LogSafe.target(web.getRequest())
				: LogSafe.clean(request.getDescription(false));
		if (errorCode.getStatus().is5xxServerError()) {
			log.error("[{}] {}", errorCode.getCode(), target, ex);
		} else {
			log.warn("[{}] {} - {}", errorCode.getCode(), target, ex.getClass().getSimpleName());
		}
		AuthAuditContext.result(errorCode.getCode());
		return ResponseEntity.status(errorCode.getStatus()).headers(headers).contentType(MediaType.APPLICATION_JSON)
				.body(ErrorResponse.of(errorCode, data));
	}

	private static ResponseEntity<ErrorResponse> respond(ErrorCode errorCode, @Nullable ErrorDetail data) {
		AuthAuditContext.result(errorCode.getCode()); // 인증 경로면 감사 로그에 결과 code 로 남는다
		return ResponseEntity.status(errorCode.getStatus()).contentType(MediaType.APPLICATION_JSON)
				.body(ErrorResponse.of(errorCode, data));
	}

	private static String reason(MessageSourceResolvable error) {
		String message = error.getDefaultMessage();
		return message == null ? "invalid" : message;
	}
}
