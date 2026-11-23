package com.app.demo.appstatus.api.dto;

import com.app.demo.global.api.ErrorDetail;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 503 MAINTENANCE 의 data. 시각은 ISO-8601 + 오프셋(예: 2026-11-23T06:00:00+09:00). */
@JsonPropertyOrder({"startedAt", "endsAt"})
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MaintenanceData implements ErrorDetail {

	/** 점검 시작 시각 (fr_dt) */
	private final String startedAt;

	/** 점검 종료 시각 (to_dt) */
	private final String endsAt;
}
