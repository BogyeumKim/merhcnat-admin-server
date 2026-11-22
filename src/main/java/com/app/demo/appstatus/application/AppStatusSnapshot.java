package com.app.demo.appstatus.application;

import com.app.demo.appstatus.domain.AppPlatform;
import com.app.demo.appstatus.domain.AppStatus;
import com.app.demo.appstatus.domain.AppStatusRepository;
import com.app.demo.appstatus.domain.VersionPolicy;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 점검 기간·버전 정책의 메모리 스냅샷. 모든 /api 요청이 참고하므로 요청마다 DB 를 읽지 않고, 10초마다 다시 읽는다.
 * <ul>
 *   <li>아직 끝나지 않은 점검 기간(진행 중 + 예약)만 읽어 둔다. 점검 여부는 요청 시각으로 판단하므로
 *       예약된 점검은 정확히 fr_dt 에 시작해 to_dt 에 끝난다. 행을 새로 넣거나 고친 것만 최대 10초 뒤 반영된다.</li>
 *   <li>다시 읽기에 실패하면 마지막 스냅샷을 유지하고 WARN 을 한 번만 남긴다(DB 장애가 곧바로 전면 차단·전면 개방으로 번지지 않게).</li>
 *   <li>기동 직후 아직 읽지 못했으면 "점검 없음, 버전 제한 없음"으로 동작한다.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppStatusSnapshot {

	private final AppStatusRepository repository;
	private final Clock clock;

	private volatile AppStatus current = AppStatus.open();
	private volatile boolean refreshFailing;

	public AppStatus current() {
		return current;
	}

	@Scheduled(fixedDelayString = "${app-status.refresh-millis:10000}")
	public void refresh() {
		try {
			AppStatus next = load(LocalDateTime.now(clock));
			logScheduleChange(current, next);
			current = next;
			if (refreshFailing) {
				refreshFailing = false;
				log.info("[APP_STATUS] 상태 다시 읽기 복구");
			}
		} catch (DataAccessException e) {
			if (!refreshFailing) {
				refreshFailing = true;
				log.warn("[APP_STATUS] 상태 다시 읽기 실패, 마지막 값을 유지합니다 - {}", e.getClass().getSimpleName());
			}
		}
	}

	private AppStatus load(LocalDateTime now) {
		Map<AppPlatform, VersionPolicy> policies = repository.findVersionPolicies().stream()
				.collect(Collectors.toMap(VersionPolicy::platform, Function.identity(), (a, b) -> a));
		return new AppStatus(repository.findEnabledMaintenanceWindowsEndingAfter(now), policies);
	}

	/** 점검 일정(진행 중 + 예약) 건수가 바뀔 때만 한 줄 남긴다 */
	private static void logScheduleChange(AppStatus before, AppStatus after) {
		int was = before.maintenanceWindows().size();
		int now = after.maintenanceWindows().size();
		if (was != now) {
			log.info("[APP_STATUS] 남은 점검 일정 {}건", now);
		}
	}
}
