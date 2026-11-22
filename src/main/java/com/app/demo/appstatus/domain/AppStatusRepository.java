package com.app.demo.appstatus.domain;

import java.time.LocalDateTime;
import java.util.List;

/** 점검 기간·필수 버전 이력 저장소 포트 */
public interface AppStatusRepository {

	/** 사용 중(use_flg = 1)이고 아직 끝나지 않은(to_dt &gt; now) 점검 기간: 진행 중 + 예약된 것. 미사용(use_flg = 0)·시작/종료가 뒤바뀐 행은 제외 */
	List<MaintenanceWindow> findEnabledMaintenanceWindowsEndingAfter(LocalDateTime now);

	/**
	 * 사용 중(use_flg = 1)인 필수 버전 중 now 시점에 판단에 필요한 것: 플랫폼별로 지금 적용 중인 1건 + 예약된(force_dt &gt; now) 것 전부.
	 * force_dt, id 오름차순. 버전 형식이 잘못된 행은 제외.
	 */
	List<VersionRelease> findEnabledVersionReleasesFrom(LocalDateTime now);
}
