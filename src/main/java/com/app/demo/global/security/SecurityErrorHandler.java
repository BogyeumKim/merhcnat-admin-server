package com.app.demo.global.security;

import com.app.demo.global.api.ApiResponse;
import com.app.demo.global.exception.CommonErrorCode;
import com.app.demo.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 필터 단계(컨트롤러 도달 전)의 인증/인가 실패를 공통 봉투로 응답한다.
 * GlobalExceptionHandler 는 컨트롤러 안의 예외만 받으므로 Spring Security 실패는 여기서 처리한다.
 * <ul>
 *   <li>토큰 없음/무효로 보호된 경로 접근 → 401 UNAUTHORIZED</li>
 *   <li>인증은 됐지만 권한 없음 → 403 FORBIDDEN</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final JsonMapper jsonMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
			throws IOException {
		write(request, response, CommonErrorCode.UNAUTHORIZED);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
			throws IOException {
		write(request, response, CommonErrorCode.FORBIDDEN);
	}

	private void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) throws IOException {
		log.warn("[{}] {} {}", errorCode.getCode(), request.getMethod(), request.getRequestURI());
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		jsonMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
	}
}
