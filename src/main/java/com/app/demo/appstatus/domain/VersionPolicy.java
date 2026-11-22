package com.app.demo.appstatus.domain;

/**
 * 플랫폼별 필수 앱 버전(app_setting 의 {PLATFORM}_MIN_VERSION). 새 버전이 나오면 항상 업데이트 필수라 버전 값은 하나뿐이다.
 *
 * @param minVersion 이보다 낮은 앱은 업데이트 필수(403 UPDATE_REQUIRED)
 */
public record VersionPolicy(AppPlatform platform, AppVersion minVersion) {
}
