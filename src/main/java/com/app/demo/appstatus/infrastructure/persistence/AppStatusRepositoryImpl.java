package com.app.demo.appstatus.infrastructure.persistence;

import com.app.demo.appstatus.domain.AppPlatform;
import com.app.demo.appstatus.domain.AppStatusRepository;
import com.app.demo.appstatus.domain.AppVersion;
import com.app.demo.appstatus.domain.MaintenanceWindow;
import com.app.demo.appstatus.domain.VersionRelease;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/** AppStatusRepository 의 MyBatis-Plus 구현 */
@Slf4j
@Repository
@RequiredArgsConstructor
class AppStatusRepositoryImpl implements AppStatusRepository {

	private static final Comparator<AppVersionEntity> RELEASE_ORDER =
			Comparator.comparing(AppVersionEntity::getForceDt).thenComparing(AppVersionEntity::getId);

	private final AppMaintenanceMapper maintenanceMapper;
	private final AppVersionMapper versionMapper;

	@Override
	public List<MaintenanceWindow> findEnabledMaintenanceWindowsEndingAfter(LocalDateTime now) {
		return maintenanceMapper.selectList(new QueryWrapper<AppMaintenanceEntity>()
						.eq("use_flg", UseFlag.USE).gt("to_dt", now).orderByAsc("fr_dt"))
				.stream().map(AppStatusRepositoryImpl::toWindow).flatMap(Optional::stream).toList();
	}

	@Override
	public List<VersionRelease> findEnabledVersionReleasesFrom(LocalDateTime now) {
		List<AppVersionEntity> rows = new ArrayList<>();
		for (AppPlatform platform : AppPlatform.values()) {
			currentRelease(platform, now).ifPresent(rows::add);
		}
		rows.addAll(scheduledReleases(now));
		return rows.stream().sorted(RELEASE_ORDER).map(AppStatusRepositoryImpl::toRelease).flatMap(Optional::stream).toList();
	}

	/** 이 플랫폼에서 지금 적용 중인 1건: force_dt ≤ now 중 가장 최근(같은 시각이면 나중에 등록한 행) */
	private Optional<AppVersionEntity> currentRelease(AppPlatform platform, LocalDateTime now) {
		return Optional.ofNullable(versionMapper.selectOne(new QueryWrapper<AppVersionEntity>()
				.eq("use_flg", UseFlag.USE).eq("platform", platform.name()).le("force_dt", now)
				.orderByDesc("force_dt").orderByDesc("id").last("limit 1")));
	}

	/** 예약된 것(force_dt > now) 전부 */
	private List<AppVersionEntity> scheduledReleases(LocalDateTime now) {
		return versionMapper.selectList(new QueryWrapper<AppVersionEntity>()
				.eq("use_flg", UseFlag.USE).gt("force_dt", now));
	}

	/** 시작/종료가 비었거나 뒤바뀐 행은 운영 실수로 보고 건너뛴다(점검으로 취급하지 않음) */
	private static Optional<MaintenanceWindow> toWindow(AppMaintenanceEntity e) {
		if (e.getFrDt() == null || e.getToDt() == null || !e.getFrDt().isBefore(e.getToDt())) {
			log.warn("[APP_STATUS] 잘못된 점검 기간 행을 건너뜁니다 - id={}", e.getId());
			return Optional.empty();
		}
		MaintenanceWindow window = MaintenanceWindow.of(e.getFrDt(), e.getToDt());
		return Optional.of(window);
	}

	/** 플랫폼·버전 형식이 잘못된 행은 운영 실수로 보고 WARN 후 건너뛴다 */
	private static Optional<VersionRelease> toRelease(AppVersionEntity e) {
		Optional<AppPlatform> platform = e.getPlatform() == null ? Optional.empty() : AppPlatform.parse(e.getPlatform());
		Optional<AppVersion> version = e.getVersion() == null ? Optional.empty() : AppVersion.parse(e.getVersion());
		if (platform.isEmpty() || version.isEmpty() || e.getForceDt() == null) {
			log.warn("[APP_STATUS] 잘못된 필수 버전 행을 건너뜁니다 - id={}", e.getId());
			return Optional.empty();
		}
		VersionRelease release = VersionRelease.with()
				.platform(platform.get())
				.version(version.get())
				.forceAt(e.getForceDt())
				.create();
		return Optional.of(release);
	}
}
