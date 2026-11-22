package com.app.demo.appstatus.infrastructure.persistence;

import com.app.demo.appstatus.domain.AppPlatform;
import com.app.demo.appstatus.domain.AppStatusRepository;
import com.app.demo.appstatus.domain.AppVersion;
import com.app.demo.appstatus.domain.MaintenanceWindow;
import com.app.demo.appstatus.domain.VersionPolicy;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/** AppStatusRepository 의 MyBatis-Plus 구현 */
@Slf4j
@Repository
@RequiredArgsConstructor
class AppStatusRepositoryImpl implements AppStatusRepository {

	private final AppMaintenanceMapper maintenanceMapper;
	private final AppSettingMapper settingMapper;

	@Override
	public List<MaintenanceWindow> findEnabledMaintenanceWindowsEndingAfter(LocalDateTime now) {
		return maintenanceMapper.selectList(new QueryWrapper<AppMaintenanceEntity>()
						.eq("use_flg", AppMaintenanceEntity.USE).gt("to_dt", now).orderByAsc("fr_dt"))
				.stream().map(AppStatusRepositoryImpl::toWindow).flatMap(Optional::stream).toList();
	}

	@Override
	public List<VersionPolicy> findVersionPolicies() {
		Map<String, String> settings = settingMapper.selectList(null).stream()
				.filter(e -> e.getSettingKey() != null && e.getSettingValue() != null)
				.collect(Collectors.toMap(AppSettingEntity::getSettingKey, AppSettingEntity::getSettingValue));
		return Arrays.stream(AppPlatform.values()).map(platform -> toPolicy(platform, settings)).flatMap(Optional::stream).toList();
	}

	/** 시작/종료가 비었거나 뒤바뀐 행은 운영 실수로 보고 건너뛴다(점검으로 취급하지 않음) */
	private static Optional<MaintenanceWindow> toWindow(AppMaintenanceEntity e) {
		if (e.getFrDt() == null || e.getToDt() == null || !e.getFrDt().isBefore(e.getToDt())) {
			log.warn("[APP_STATUS] 잘못된 점검 기간 행을 건너뜁니다 - id={}", e.getId());
			return Optional.empty();
		}
		return Optional.of(new MaintenanceWindow(e.getFrDt(), e.getToDt()));
	}

	/** 설정이 없으면 버전 제한 없음, 형식이 잘못됐으면 운영 실수로 보고 WARN 후 제한 없음으로 둔다 */
	private static Optional<VersionPolicy> toPolicy(AppPlatform platform, Map<String, String> settings) {
		String key = platform.minVersionSettingKey();
		String value = settings.get(key);
		if (value == null) {
			return Optional.empty();
		}
		Optional<AppVersion> min = AppVersion.parse(value);
		if (min.isEmpty()) {
			log.warn("[APP_STATUS] 잘못된 버전 설정을 건너뜁니다 - key={}", key);
			return Optional.empty();
		}
		return Optional.of(new VersionPolicy(platform, min.get()));
	}
}
