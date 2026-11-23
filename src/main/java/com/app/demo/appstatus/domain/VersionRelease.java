package com.app.demo.appstatus.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 필수 앱 버전 이력(app_version 한 행). forceAt 부터 이 버전이 필수가 된다(새 버전이 나오면 항상 업데이트 필수).
 * forceAt(force_dt) 은 스토어 출시 시각이 아니라 필수 적용 시각이다. 스토어 공개(단계적 출시라면 100% 배포) 후 유예 기간(예: 7일)을 두고 잡는다. 그 전에 걸면 받을 수 없는 버전으로 업데이트하라는 화면에 갇힌다.
 */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VersionRelease {

	private final AppPlatform platform;

	/** 이보다 낮은 앱은 업데이트 필수(403 UPDATE_REQUIRED) */
	private final AppVersion version;

	/** 필수 적용 시작 시각 (force_dt, 이 시각 포함) */
	private final LocalDateTime forceAt;

	public boolean isEffectiveAt(LocalDateTime now) {
		return !forceAt.isAfter(now);
	}
}
