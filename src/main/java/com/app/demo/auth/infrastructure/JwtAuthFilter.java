package com.app.demo.auth.infrastructure;

import com.app.demo.auth.domain.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code Authorization: Bearer <Access-Token>} 검증 필터.
 * 토큰이 유효하면 SecurityContext 에 인증 정보를 채우고, 없거나 무효면 인증 없이 통과시킨다
 * (인증이 필요한 경로에서는 이후 401 이 내려간다).
 */
class JwtAuthFilter extends OncePerRequestFilter {

	private static final String BEARER = "Bearer ";

	private final TokenProvider tokens;

	JwtAuthFilter(TokenProvider tokens) {
		this.tokens = tokens;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith(BEARER)) {
			tokens.parseAccessToken(header.substring(BEARER.length())).ifPresent(user ->
					SecurityContextHolder.getContext().setAuthentication(
							new UsernamePasswordAuthenticationToken(user, null, java.util.List.of())));
		}
		chain.doFilter(request, response);
	}
}
