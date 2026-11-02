package com.app.demo.device.presentation;

import org.jspecify.annotations.Nullable;

/** 푸시 토큰 갱신 요청. 보낸 필드만 갱신한다. */
public record DeviceUpdateRequest(@Nullable String expoPushToken) {
}
