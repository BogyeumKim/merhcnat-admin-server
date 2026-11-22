package com.app.demo.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 인증 감사 로그: 인증 관련 요청 1건당 결과를 {@code AUTH_AUDIT} 로거에 한 줄로 남긴다.
 * <pre>event=LOGIN result=MFA_REQUIRED status=200 user=mfa**ll ip=10.0.0.1 device=3fa2b1c9</pre>
 * 시각은 로그 패턴이 붙인다. 비밀번호·OTP·토큰·연락처·기기 비밀키는 절대 남기지 않는다.
 * Spring Security 필터보다 바깥에서 동작해 401(토큰 없음)까지 기록하고, IP 는 프록시 뒤에서도
 * {@code server.forward-headers-strategy: framework} 로 X-Forwarded-For 가 반영된 값을 쓴다.
 */
@Slf4j(topic = "AUTH_AUDIT") // 감사 로그는 전용 로거 이름으로 분리해 별도 파일·수집기로 보낼 수 있게 한다
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthAuditFilter extends OncePerRequestFilter {

	private static final Map<String, String> EVENTS = Map.of(
			"POST /api/auth/login", "LOGIN",
			"POST /api/auth/otp/send", "OTP_SEND",
			"POST /api/auth/otp/verify", "OTP_VERIFY",
			"POST /api/auth/password/temp", "PASSWORD_TEMP",
			"PUT /api/users/me/password", "PASSWORD_CHANGE",
			"PUT /api/devices/me", "DEVICE_PUSH_UPDATE");

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return event(request) == null;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		try {
			chain.doFilter(request, response);
		} finally {
			int status = response.getStatus();
			log.info("event={} result={} status={} user={} ip={} device={}", event(request),
					resultCode(request, status), status, attribute(request, AuthAuditContext.USER),
					request.getRemoteAddr(), attribute(request, AuthAuditContext.DEVICE));
		}
	}

	private static @Nullable String event(HttpServletRequest request) {
		return EVENTS.get(request.getMethod() + " " + request.getRequestURI());
	}

	/** 처리 중 기록된 code. 없으면 200 은 SUCCESS, 그 외는 HTTP status 로 남긴다. */
	private static String resultCode(HttpServletRequest request, int status) {
		Object recorded = request.getAttribute(AuthAuditContext.RESULT);
		if (recorded != null) {
			return recorded.toString();
		}
		return status == HttpServletResponse.SC_OK ? "SUCCESS" : "HTTP_" + status;
	}

	private static String attribute(HttpServletRequest request, String name) {
		return Objects.toString(request.getAttribute(name), "-");
	}
}
