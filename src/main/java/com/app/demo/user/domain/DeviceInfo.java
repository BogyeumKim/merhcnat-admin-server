package com.app.demo.user.domain;

import com.app.demo.global.exception.BusinessException;
import com.app.demo.user.exception.UserErrorCode;
import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * 기기 정보 값 객체. 생성 시점에 필수값·형식을 검증하므로(fail-fast) 만들어진 DeviceInfo 는 항상 유효하다.
 * <ul>
 *   <li>deviceId: 앱이 최초 실행 때 만든 UUID. 비었거나 64자 초과 → 400 DEVICE_REQUIRED</li>
 *   <li>platform: IOS | ANDROID (대소문자 무시, 대문자로 정규화). 그 외 → 400 DEVICE_INVALID</li>
 *   <li>osVersion/deviceModel/appVersion: 선택값. DB 컬럼 길이에 맞게 자른다.</li>
 * </ul>
 */
public record DeviceInfo(
		String deviceId,
		String platform,
		@Nullable String osVersion,
		@Nullable String deviceModel,
		@Nullable String appVersion) {

	private static final int MAX_DEVICE_ID_LENGTH = 64;
	private static final Set<String> PLATFORMS = Set.of("IOS", "ANDROID");

	public DeviceInfo {
		if (deviceId.isBlank() || deviceId.length() > MAX_DEVICE_ID_LENGTH) {
			throw new BusinessException(UserErrorCode.DEVICE_REQUIRED);
		}
		platform = platform.toUpperCase(Locale.ROOT);
		if (!PLATFORMS.contains(platform)) {
			throw new BusinessException(UserErrorCode.DEVICE_INVALID);
		}
		osVersion = truncate(osVersion, 30);
		deviceModel = truncate(deviceModel, 60);
		appVersion = truncate(appVersion, 30);
	}

	private static @Nullable String truncate(@Nullable String value, int maxLength) {
		return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
	}
}
