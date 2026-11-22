package com.app.demo.appstatus.domain;

import java.util.Arrays;
import java.util.Optional;

/** 앱 플랫폼 (X-App-Platform 헤더). 필수 버전 설정 키는 {@link #minVersionSettingKey()} */
public enum AppPlatform {
	IOS,
	ANDROID;

	/** 헤더 값 → 플랫폼(대소문자 무시). 알 수 없으면 empty */
	public static Optional<AppPlatform> parse(String value) {
		return Arrays.stream(values()).filter(p -> p.name().equalsIgnoreCase(value.strip())).findFirst();
	}

	/** app_setting 에서 이 플랫폼의 필수 버전을 담는 키 (예: IOS_MIN_VERSION) */
	public String minVersionSettingKey() {
		return name() + "_MIN_VERSION";
	}
}
