package com.app.demo.appstatus.application;

import com.app.demo.appstatus.domain.MaintenanceWindow;
import com.app.demo.appstatus.domain.VersionPolicy;

/** 요청을 통과시킬지에 대한 판단 결과 */
public sealed interface AccessDecision {

	/** 통과 */
	record Allowed() implements AccessDecision {
	}

	/** 점검 중 → 503 MAINTENANCE */
	record UnderMaintenance(MaintenanceWindow window) implements AccessDecision {
	}

	/** 필수 버전 미만 → 403 UPDATE_REQUIRED */
	record UpdateRequired(VersionPolicy policy) implements AccessDecision {
	}

	/** 앱 버전 헤더가 한쪽만 있거나 형식이 잘못됨 → 400 BAD_REQUEST */
	record InvalidClientHeaders() implements AccessDecision {
	}
}
