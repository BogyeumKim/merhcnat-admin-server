package com.app.demo.global.security;

import com.app.demo.global.api.ErrorResponseWriter;
import com.app.demo.global.exception.CommonErrorCode;
import com.app.demo.global.exception.ErrorCode;
import com.app.demo.global.exception.LogSafe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.stereotype.Component;

/**
 * 필터 단계(컨트롤러 도달 전)의 인증/인가 실패를 공통 봉투로 응답한다.
 * GlobalExceptionHandler 는 컨트롤러 안의 예외만 받으므로 Spring Security 실패는 여기서 처리한다.
 * <ul>
 *   <li>토큰 없음/무효로 보호된 경로 접근 → 401 UNAUTHORIZED</li>
 *   <li>토큰은 유효하지만 세션이 거부됨 → JWT 필터가 {@link #reject} 로 남긴 사유(SESSION_EXPIRED / SESSION_REPLACED / 503 SESSION_STORE_UNAVAILABLE)</li>
 *   <li>인증은 됐지만 권한 없음 → 403 FORBIDDEN</li>
 *   <li>방화벽(StrictHttpFirewall)이 거부한 비정상 요청(`//`, `;`, 인코딩된 `..`·`/`, 허용되지 않은 메서드·헤더 문자 등) → 400 BAD_REQUEST</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler, RequestRejectedHandler {

	private static final String REJECTION = SecurityErrorHandler.class.getName() + ".rejection";

	private final ErrorResponseWriter writer;

	/** 인증을 거부한 사유를 남긴다. 보호된 경로라면 이후 401 응답에 이 code 가 쓰인다. */
	public static void reject(HttpServletRequest request, ErrorCode reason) {
		request.setAttribute(REJECTION, reason);
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
			throws IOException {
		write(request, response, request.getAttribute(REJECTION) instanceof ErrorCode reason ? reason : CommonErrorCode.UNAUTHORIZED);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
			throws IOException {
		write(request, response, CommonErrorCode.FORBIDDEN);
	}

	/** Spring Security 가 이 타입의 빈을 찾아 방화벽 거부 응답에 쓴다(기본 동작은 sendError(400) → HTML/오류 페이지). */
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, RequestRejectedException e)
			throws IOException {
		write(request, response, CommonErrorCode.MALFORMED_REQUEST);
	}

	private void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) throws IOException {
		log.warn("[{}] {}", errorCode.getCode(), LogSafe.target(request));
		request.setAttribute(AuthAuditContext.RESULT, errorCode.getCode());
		writer.write(response, errorCode);
	}
}
