package com.app.demo.user.domain;

import org.jspecify.annotations.Nullable;

/**
 * 기기 정보 값 객체. deviceId 는 앱이 최초 실행 때 만든 UUID.
 *
 * @param platform IOS | ANDROID
 */
public record DeviceInfo(
		String deviceId,
		String platform,
		@Nullable String osVersion,
		@Nullable String deviceModel,
		@Nullable String appVersion) {
}
