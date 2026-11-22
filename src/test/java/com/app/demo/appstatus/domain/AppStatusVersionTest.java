package com.app.demo.appstatus.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 필수 버전 판단: 플랫폼별로 force_dt ≤ now 인 것 중 가장 최근 것 (예약분은 force_dt 정각부터) */
class AppStatusVersionTest {

	private static final LocalDateTime RELEASE = LocalDateTime.of(2026, 12, 1, 10, 0);

	private static VersionRelease release(AppPlatform platform, String version, LocalDateTime at) {
		return new VersionRelease(platform, AppVersion.parse(version).orElseThrow(), at);
	}

	/** 저장소가 주는 순서(force_dt, id 오름차순) 그대로 넣는다 */
	private static AppStatus status(VersionRelease... releases) {
		return new AppStatus(List.of(), List.of(releases));
	}

	private static final VersionRelease IOS_1_0 = release(AppPlatform.IOS, "1.0.0", RELEASE.minusMonths(1));
	private static final VersionRelease IOS_1_1 = release(AppPlatform.IOS, "1.1.0", RELEASE);
	private static final VersionRelease ANDROID_1_0 = release(AppPlatform.ANDROID, "1.0.0", RELEASE.minusMonths(1));

	@Test
	@DisplayName("예약된 버전은 적용 시각 전에는 쓰이지 않고 이전 버전이 필수다")
	void scheduledNotYetEffective() {
		assertThat(status(IOS_1_0, IOS_1_1).requiredVersion(AppPlatform.IOS, RELEASE.minusSeconds(1))).contains(IOS_1_0);
	}

	@Test
	@DisplayName("적용 시각 정각부터 새 버전이 필수다 (시각 포함)")
	void effectiveAtReleaseInclusive() {
		assertThat(status(IOS_1_0, IOS_1_1).requiredVersion(AppPlatform.IOS, RELEASE)).contains(IOS_1_1);
	}

	@Test
	@DisplayName("같은 적용 시각이면 나중에 등록한 행이 이긴다")
	void sameReleaseTimeLaterRowWins() {
		VersionRelease fix = release(AppPlatform.IOS, "1.1.1", RELEASE);
		assertThat(status(IOS_1_0, IOS_1_1, fix).requiredVersion(AppPlatform.IOS, RELEASE.plusHours(1))).contains(fix);
	}

	@Test
	@DisplayName("플랫폼별로 따로 판단한다")
	void perPlatform() {
		AppStatus status = status(IOS_1_0, ANDROID_1_0, IOS_1_1);
		assertThat(status.requiredVersion(AppPlatform.ANDROID, RELEASE.plusDays(1))).contains(ANDROID_1_0);
		assertThat(status.requiredVersion(AppPlatform.IOS, RELEASE.plusDays(1))).contains(IOS_1_1);
	}

	@Test
	@DisplayName("적용 중인 버전이 없으면 버전 제한 없음")
	void noneEffective() {
		assertThat(status(IOS_1_1).requiredVersion(AppPlatform.IOS, RELEASE.minusDays(1))).isEmpty();
		assertThat(AppStatus.open().requiredVersion(AppPlatform.IOS, RELEASE)).isEmpty();
	}
}
