package com.app.demo.appstatus.domain;

import java.time.LocalDateTime;

/**
 * 점검 기간(app_maintenance 한 행). fr_dt ≤ 현재 &lt; to_dt 이면 점검 중이다(종료 시각은 포함하지 않는다).
 *
 * @param from 점검 시작 시각 (fr_dt)
 * @param to   점검 종료 시각 (to_dt)
 */
public record MaintenanceWindow(LocalDateTime from, LocalDateTime to) {

	public MaintenanceWindow {
		if (!from.isBefore(to)) {
			throw new IllegalArgumentException("점검 시작 시각은 종료 시각보다 앞서야 합니다.");
		}
	}

	public boolean contains(LocalDateTime now) {
		return !now.isBefore(from) && now.isBefore(to);
	}
}
