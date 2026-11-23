package com.app.demo.appstatus.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 요청마다 참고하는 앱 상태 스냅샷(점검 기간 목록 + 필수 버전 이력). 메모리에 두고 주기적으로 갱신한다.
 * 점검·필수 버전 모두 스냅샷을 만든 시각이 아니라 요청 시각으로 판단하므로, 예약된 점검·버전은 정확히 그 시각에 반영된다.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AppStatus {

	private final List<MaintenanceWindow> maintenanceWindows;

	/** force_dt, id 오름차순 (같은 시각이면 나중에 등록한 행이 이긴다) */
	private final List<VersionRelease> versionReleases;

	/** 두 목록을 복사해 스냅샷을 만든다. */
	public static AppStatus of(List<MaintenanceWindow> maintenanceWindows, List<VersionRelease> versionReleases) {
		List<MaintenanceWindow> windows = List.copyOf(maintenanceWindows);
		List<VersionRelease> releases = List.copyOf(versionReleases);
		return new AppStatus(windows, releases);
	}

	/** DB 를 아직 읽지 못했을 때의 기본값: 점검 없음, 버전 제한 없음 */
	public static AppStatus open() {
		return new AppStatus(List.of(), List.of());
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
		LocalDateTime startedAt = active.stream().map(MaintenanceWindow::getFrom).min(Comparator.naturalOrder()).orElseThrow();
		LocalDateTime endsAt = active.stream().map(MaintenanceWindow::getTo).max(Comparator.naturalOrder()).orElseThrow();
		MaintenanceWindow merged = MaintenanceWindow.of(startedAt, endsAt);
		return Optional.of(merged);
	}

	/** now 에 적용 중인 필수 버전: 이 플랫폼에서 force_dt ≤ now 인 것 중 가장 최근 것. 없으면 버전 제한 없음 */
	public Optional<VersionRelease> requiredVersion(AppPlatform platform, LocalDateTime now) {
		return versionReleases.stream()
				.filter(release -> release.getPlatform() == platform && release.isEffectiveAt(now))
				.reduce((earlier, later) -> later);
	}
}
