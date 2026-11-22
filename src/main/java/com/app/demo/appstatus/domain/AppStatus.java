package com.app.demo.appstatus.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 요청마다 참고하는 앱 상태 스냅샷(점검 기간 목록 + 플랫폼별 버전 정책). 메모리에 두고 주기적으로 갱신한다.
 * 점검 여부는 스냅샷을 만든 시각이 아니라 요청 시각으로 판단하므로, 예약된 점검은 정확히 시작/종료 시각에 반영된다.
 */
public record AppStatus(List<MaintenanceWindow> maintenanceWindows, Map<AppPlatform, VersionPolicy> versionPolicies) {

	public AppStatus {
		maintenanceWindows = List.copyOf(maintenanceWindows);
		versionPolicies = Map.copyOf(versionPolicies);
	}

	/** DB 를 아직 읽지 못했을 때의 기본값: 점검 없음, 버전 제한 없음 */
	public static AppStatus open() {
		return new AppStatus(List.of(), Map.of());
	}

	/**
	 * now 에 진행 중인 점검. 여러 기간이 겹치면 하나로 합쳐 가장 이른 시작 ~ 가장 늦은 종료를 돌려준다
	 * (앱에는 "언제부터 언제까지 점검"만 보여 주면 되고, 겹친 기간이 모두 끝나야 실제로 이용할 수 있기 때문).
	 */
	public Optional<MaintenanceWindow> activeMaintenance(LocalDateTime now) {
		List<MaintenanceWindow> active = maintenanceWindows.stream().filter(w -> w.contains(now)).toList();
		if (active.isEmpty()) {
			return Optional.empty();
		}
		LocalDateTime startedAt = active.stream().map(MaintenanceWindow::from).min(Comparator.naturalOrder()).orElseThrow();
		LocalDateTime endsAt = active.stream().map(MaintenanceWindow::to).max(Comparator.naturalOrder()).orElseThrow();
		return Optional.of(new MaintenanceWindow(startedAt, endsAt));
	}

	public Optional<VersionPolicy> versionPolicy(AppPlatform platform) {
		return Optional.ofNullable(versionPolicies.get(platform));
	}
}
