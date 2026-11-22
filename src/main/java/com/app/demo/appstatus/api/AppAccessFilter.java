package com.app.demo.appstatus.api;

import com.app.demo.appstatus.api.dto.MaintenanceData;
import com.app.demo.appstatus.api.dto.UpdateRequiredData;
import com.app.demo.appstatus.application.AccessDecision;
import com.app.demo.appstatus.application.AppAccessService;
import com.app.demo.appstatus.config.AppStatusProperties;
import com.app.demo.appstatus.domain.MaintenanceWindow;
import com.app.demo.appstatus.domain.VersionPolicy;
import com.app.demo.appstatus.exception.AppStatusErrorCode;
import com.app.demo.global.api.ErrorResponseWriter;
import com.app.demo.global.exception.CommonErrorCode;
import com.app.demo.global.exception.LogSafe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 모든 /api 요청(로그인 전/후)의 입구: 점검 기간이면 503 MAINTENANCE, 앱 버전이 필수 버전보다 낮으면 403 UPDATE_REQUIRED.
 * 점검을 적용할 경로는 app-status.maintenance-path-prefix 한 곳에서 정한다(지금 "/api/" = 전체, "/api/auth/" 로 바꾸면 로그인 단계만).
 * 본문 크기 제한 다음, 감사 로그·보안 필터보다 먼저 실행해 점검 중에는 인증 처리까지 가지 않게 한다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@RequiredArgsConstructor
public class AppAccessFilter extends OncePerRequestFilter {

	static final String VERSION_HEADER = "X-App-Version";
	static final String PLATFORM_HEADER = "X-App-Platform";

	private final AppAccessService access;
	private final AppStatusProperties properties;
	private final ErrorResponseWriter writer;
	private final Clock clock;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		boolean maintenanceApplies = request.getRequestURI().startsWith(properties.maintenancePathPrefix());
		switch (access.decide(maintenanceApplies, request.getHeader(PLATFORM_HEADER), request.getHeader(VERSION_HEADER))) {
			case AccessDecision.Allowed() -> chain.doFilter(request, response);
			case AccessDecision.UnderMaintenance(var window) -> writeMaintenance(response, window);
			case AccessDecision.UpdateRequired(var policy) -> writeUpdateRequired(response, policy);
			case AccessDecision.InvalidClientHeaders() -> {
				log.warn("[{}] {} - invalid app version headers", CommonErrorCode.INVALID_INPUT.getCode(), LogSafe.target(request));
				writer.write(response, CommonErrorCode.INVALID_INPUT);
			}
		}
	}

	private void writeMaintenance(HttpServletResponse response, MaintenanceWindow window) throws IOException {
		writer.write(response, AppStatusErrorCode.MAINTENANCE, AppStatusErrorCode.MAINTENANCE.getMessage(),
				new MaintenanceData(isoWithOffset(window.from()), isoWithOffset(window.to())));
	}

	private void writeUpdateRequired(HttpServletResponse response, VersionPolicy policy) throws IOException {
		writer.write(response, AppStatusErrorCode.UPDATE_REQUIRED, AppStatusErrorCode.UPDATE_REQUIRED.getMessage(),
				new UpdateRequiredData(policy.minVersion().toString()));
	}

	private String isoWithOffset(LocalDateTime time) {
		return time.atZone(clock.getZone()).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
