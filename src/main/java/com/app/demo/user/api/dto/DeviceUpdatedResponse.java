package com.app.demo.user.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 푸시 토큰 갱신 성공(200) 본문. 빈 본문이면 앱의 {@code res.json()} 이 실패하므로 항상 JSON 을 돌려준다. ex) {@code {"updated": true}} */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceUpdatedResponse {

	/** 갱신 완료 여부(성공 응답이므로 항상 true) */
	private final boolean updated;

	public static DeviceUpdatedResponse success() {
		return DeviceUpdatedResponse.with().updated(true).create();
	}
}
