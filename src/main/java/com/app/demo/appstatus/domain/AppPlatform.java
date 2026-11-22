package com.app.demo.appstatus.domain;

import java.util.Arrays;
import java.util.Optional;

/** 앱 플랫폼 (X-App-Platform 헤더, app_version.platform) */
public enum AppPlatform {
	IOS,
	ANDROID;

	/** 헤더 값 → 플랫폼(대소문자 무시). 알 수 없으면 empty */
	public static Optional<AppPlatform> parse(String value) {
		return Arrays.stream(values()).filter(p -> p.name().equalsIgnoreCase(value.strip())).findFirst();
	}
}
