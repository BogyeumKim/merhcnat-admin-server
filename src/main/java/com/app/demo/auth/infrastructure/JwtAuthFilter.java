package com.app.demo.auth.infrastructure;

import com.app.demo.auth.application.SessionService;
import com.app.demo.auth.domain.IdMasking;
import com.app.demo.auth.domain.TokenProvider;
import com.app.demo.global.exception.CommonErrorCode;
import com.app.demo.global.exception.LogSafe;
import com.app.demo.global.security.AuthAuditContext;
import com.app.demo.global.security.AuthUser;
import com.app.demo.global.security.SecurityErrorHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code Authorization: Bearer <Access-Token>} 검증 필터.
 * 서명·만료·aud 가 맞고 토큰의 jti 가 현재 로그인 세션(Redis)과 같을 때만 인증 정보를 채운다(통과 시 무활동 만료 연장).
 * 그 외에는 인증 없이 통과시키고 거부 사유만 남긴다 → 보호된 경로라면 SecurityErrorHandler 가 그 사유로 응답한다.
 * 세션 저장소에 접근할 수 없으면 인증하지 않는다(fail-closed, 503).
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

	private static final String BEARER = "Bearer ";

	private final TokenProvider tokens;
	private final SessionService sessions;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith(BEARER)) {
			tokens.parseAccessToken(header.substring(BEARER.length())).ifPresent(user -> authenticateIfCurrent(request, user));
		}
		chain.doFilter(request, response);
	}

	/** 토큰이 현재 세션의 토큰이면 인증, 아니면 거부 사유를 남긴다. */
	private void authenticateIfCurrent(HttpServletRequest request, AuthUser user) {
		AuthAuditContext.subject(IdMasking.mask(user.getUserId()), user.getDeviceIdHash());
		try {
			sessions.verify(user).ifPresentOrElse(
					reason -> SecurityErrorHandler.reject(request, reason),
					() -> SecurityContextHolder.getContext()
							.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of())));
		} catch (DataAccessException e) {
			log.error("[SESSION_STORE_UNAVAILABLE] {} - {}", LogSafe.target(request), e.getClass().getSimpleName());
			SecurityErrorHandler.reject(request, CommonErrorCode.SESSION_STORE_UNAVAILABLE);
		}
	}
}
