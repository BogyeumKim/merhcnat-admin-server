package com.app.demo.appstatus.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 점검·버전 차단 설정 (prefix: app-status).
 *
 * @param maintenancePathPrefix 점검(503)을 적용할 경로 접두사. "/api/" 면 로그인 전/후 모든 API, "/api/auth/" 로 바꾸면 로그인 단계만.
 *                              버전 차단(403 UPDATE_REQUIRED)은 이 값과 관계없이 모든 /api 요청에 적용된다.
 */
@Validated
@ConfigurationProperties(prefix = "app-status")
public record AppStatusProperties(@NotBlank String maintenancePathPrefix) {
}
