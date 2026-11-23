package com.app.demo.appstatus.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** 점검 기간(app_maintenance 한 행). fr_dt ≤ 현재 &lt; to_dt 이면 점검 중이다(종료 시각은 포함하지 않는다). */
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MaintenanceWindow {

	/** 점검 시작 시각 (fr_dt) */
	private final LocalDateTime from;

	/** 점검 종료 시각 (to_dt) */
	private final LocalDateTime to;

	/** 시작이 종료보다 앞설 때만 만든다. ex) 06:00~06:00 → IllegalArgumentException */
	public static MaintenanceWindow of(LocalDateTime from, LocalDateTime to) {
		if (!from.isBefore(to)) {
			throw new IllegalArgumentException("점검 시작 시각은 종료 시각보다 앞서야 합니다.");
		}
		return new MaintenanceWindow(from, to);
	}

	public boolean contains(LocalDateTime now) {
		return !now.isBefore(from) && now.isBefore(to);
	}
}
