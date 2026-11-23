package com.app.demo.user.domain;

import com.app.demo.global.exception.BusinessException;
import com.app.demo.user.exception.UserErrorCode;
import java.util.Locale;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * 기기 정보 값 객체. {@link #of} 가 필수값·형식을 검증하므로(fail-fast) 만들어진 DeviceInfo 는 항상 유효하다.
 * deviceId(앱이 최초 실행 때 만든 UUID) 비었거나 64자 초과 → 400 DEVICE_REQUIRED, platform 이 IOS | ANDROID(대소문자 무시) 아니면 → 400 DEVICE_INVALID.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceInfo {

	private static final int MAX_DEVICE_ID_LENGTH = 64;
	private static final Set<String> PLATFORMS = Set.of("IOS", "ANDROID");

	private final String deviceId;

	/** IOS | ANDROID (대문자로 정규화) */
	private final String platform;

	/** 선택값. DB 컬럼 길이(30)에 맞게 자른다 */
	private final @Nullable String osVersion;

	/** 선택값. DB 컬럼 길이(60)에 맞게 자른다 */
	private final @Nullable String deviceModel;

	/** 선택값. DB 컬럼 길이(30)에 맞게 자른다 */
	private final @Nullable String appVersion;

	/** 검증·정규화해 만든다. ex) ("uuid", "ios", ...) → platform {@code IOS}, ("", "IOS", ...) → DEVICE_REQUIRED */
	public static DeviceInfo of(String deviceId, String platform, @Nullable String osVersion, @Nullable String deviceModel,
			@Nullable String appVersion) {
		if (deviceId.isBlank() || deviceId.length() > MAX_DEVICE_ID_LENGTH) {
			throw new BusinessException(UserErrorCode.DEVICE_REQUIRED);
		}
		String upperPlatform = platform.toUpperCase(Locale.ROOT);
		if (!PLATFORMS.contains(upperPlatform)) {
			throw new BusinessException(UserErrorCode.DEVICE_INVALID);
		}
		String shortOsVersion = truncate(osVersion, 30);
		String shortDeviceModel = truncate(deviceModel, 60);
		String shortAppVersion = truncate(appVersion, 30);
		return new DeviceInfo(deviceId, upperPlatform, shortOsVersion, shortDeviceModel, shortAppVersion);
	}

	private static @Nullable String truncate(@Nullable String value, int maxLength) {
		return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
	}
}
