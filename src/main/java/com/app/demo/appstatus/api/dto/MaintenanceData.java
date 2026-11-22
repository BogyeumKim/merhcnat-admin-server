package com.app.demo.appstatus.api.dto;

import com.app.demo.global.api.ErrorDetail;

/**
 * 503 MAINTENANCE 의 data. 시각은 ISO-8601 + 오프셋(예: 2026-11-23T06:00:00+09:00).
 *
 * @param startedAt 점검 시작 시각 (fr_dt)
 * @param endsAt    점검 종료 시각 (to_dt)
 */
public record MaintenanceData(String startedAt, String endsAt) implements ErrorDetail {
}
