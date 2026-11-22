package com.app.demo.appstatus.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 점검 기간(fr_dt ≤ now &lt; to_dt) 판단 */
class AppStatusTest {

	private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 23, 2, 0);

	private static AppStatus status(MaintenanceWindow... windows) {
		return new AppStatus(List.of(windows), List.of());
	}

	private static MaintenanceWindow window(int fromHour, int toHour) {
		return new MaintenanceWindow(T0.withHour(fromHour), T0.withHour(toHour));
	}

	@Test
	@DisplayName("점검 기간 안이면 그 기간을 돌려준다")
	void activeWindow() {
		assertThat(status(window(2, 6)).activeMaintenance(T0.withHour(3))).contains(window(2, 6));
	}

	@Test
	@DisplayName("시작 시각 정각부터 점검이다 (시작 포함)")
	void startInclusive() {
		assertThat(status(window(2, 6)).activeMaintenance(T0)).isPresent();
	}

	@Test
	@DisplayName("시작 전이면 점검이 아니다")
	void beforeStart() {
		assertThat(status(window(2, 6)).activeMaintenance(T0.minusSeconds(1))).isEmpty();
	}

	@Test
	@DisplayName("종료 시각 정각에는 이미 점검이 아니다 (종료 미포함)")
	void endExclusive() {
		assertThat(status(window(2, 6)).activeMaintenance(T0.withHour(6))).isEmpty();
		assertThat(status(window(2, 6)).activeMaintenance(T0.withHour(6).minusNanos(1))).isPresent();
	}

	@Test
	@DisplayName("겹친 기간은 가장 이른 시작 ~ 가장 늦은 종료로 합친다")
	void overlappingWindowsMerged() {
		assertThat(status(window(2, 4), window(3, 6), window(8, 9)).activeMaintenance(T0.withHour(3)))
				.contains(window(2, 6));
	}

	@Test
	@DisplayName("점검 기간이 없으면 점검이 아니다")
	void none() {
		assertThat(AppStatus.open().activeMaintenance(T0)).isEmpty();
	}

	@Test
	@DisplayName("시작이 종료보다 늦거나 같으면 만들 수 없다")
	void invalidWindow() {
		assertThatThrownBy(() -> window(6, 2)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> window(2, 2)).isInstanceOf(IllegalArgumentException.class);
	}
}
