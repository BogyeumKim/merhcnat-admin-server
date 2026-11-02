package com.app.demo.auth.presentation;

import com.app.demo.user.domain.DeviceInfo;
import org.jspecify.annotations.Nullable;

/** 앱이 보내는 기기 정보. deviceId 는 앱이 최초 실행 때 만든 UUID. */
public record DeviceRequest(
		@Nullable String deviceId,
		@Nullable String platform,
		@Nullable String osVersion,
		@Nullable String deviceModel,
		@Nullable String appVersion) {

	/** 도메인 값 객체로 변환 (누락된 필수값은 빈 문자열 → 서비스에서 DEVICE_REQUIRED/INVALID 처리) */
	DeviceInfo toDomain() {
		return new DeviceInfo(deviceId == null ? "" : deviceId, platform == null ? "" : platform,
				osVersion, deviceModel, appVersion);
	}
}
