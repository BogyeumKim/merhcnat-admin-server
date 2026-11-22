package com.app.demo.auth.api.dto;

import com.app.demo.user.domain.DeviceInfo;
import org.jspecify.annotations.Nullable;

/**
 * 앱이 보내는 기기 정보. deviceId 는 앱이 최초 실행 때 만든 UUID.
 * 필수·형식 검증은 앱이 분기하는 DEVICE_REQUIRED / DEVICE_INVALID 코드를 유지하기 위해 {@link DeviceInfo} 가 생성 시점에 한다.
 */
public record DeviceRequest(
		@Nullable String deviceId,
		@Nullable String platform,
		@Nullable String osVersion,
		@Nullable String deviceModel,
		@Nullable String appVersion) {

	/** 기기 정보를 아예 보내지 않은 요청. 도메인 변환 시 DEVICE_REQUIRED 로 거부된다. */
	static final DeviceRequest MISSING = new DeviceRequest(null, null, null, null, null);

	/** 도메인 값 객체로 변환. 누락된 필수값은 빈 문자열로 넘겨 {@link DeviceInfo} 가 거부하게 한다. */
	public DeviceInfo toDomain() {
		return new DeviceInfo(nullToEmpty(deviceId), nullToEmpty(platform), osVersion, deviceModel, appVersion);
	}

	private static String nullToEmpty(@Nullable String value) {
		return value == null ? "" : value;
	}
}
