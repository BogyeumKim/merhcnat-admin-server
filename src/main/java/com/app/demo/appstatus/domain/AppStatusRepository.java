package com.app.demo.appstatus.domain;

import java.time.LocalDateTime;
import java.util.List;

/** 점검 기간·버전 정책 저장소 포트 */
public interface AppStatusRepository {

	/** 사용 중(use_flg = 1)이고 아직 끝나지 않은(to_dt &gt; now) 점검 기간: 진행 중 + 예약된 것. 미사용(use_flg = 0)·시작/종료가 뒤바뀐 행은 제외 */
	List<MaintenanceWindow> findEnabledMaintenanceWindowsEndingAfter(LocalDateTime now);

	/** 플랫폼별 필수 버전(app_setting). 값이 없거나 형식이 잘못된 플랫폼은 제외(그 플랫폼은 버전 제한 없음) */
	List<VersionPolicy> findVersionPolicies();
}
