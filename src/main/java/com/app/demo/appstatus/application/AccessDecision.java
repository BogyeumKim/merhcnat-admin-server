package com.app.demo.appstatus.application;

import com.app.demo.appstatus.domain.MaintenanceWindow;
import com.app.demo.appstatus.domain.VersionRelease;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 요청을 통과시킬지에 대한 판단 결과 */
public sealed interface AccessDecision {

	/** 통과 */
	final class Allowed implements AccessDecision {

		private static final Allowed INSTANCE = new Allowed();

		private Allowed() {
		}

		public static Allowed of() {
			return INSTANCE;
		}
	}

	/** 점검 중 → 503 MAINTENANCE */
	@Getter
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	final class UnderMaintenance implements AccessDecision {

		private final MaintenanceWindow window;

		public static UnderMaintenance of(MaintenanceWindow window) {
			return new UnderMaintenance(window);
		}
	}

	/** 필수 버전 미만 → 403 UPDATE_REQUIRED */
	@Getter
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	final class UpdateRequired implements AccessDecision {

		private final VersionRelease required;

		public static UpdateRequired of(VersionRelease required) {
			return new UpdateRequired(required);
		}
	}

	/** 앱 버전 헤더가 한쪽만 있거나 형식이 잘못됨 → 400 BAD_REQUEST */
	final class InvalidClientHeaders implements AccessDecision {

		private static final InvalidClientHeaders INSTANCE = new InvalidClientHeaders();

		private InvalidClientHeaders() {
		}

		public static InvalidClientHeaders of() {
			return INSTANCE;
		}
	}
}
