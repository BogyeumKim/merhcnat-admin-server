package com.app.demo.auth.presentation;

import org.jspecify.annotations.Nullable;

/** 1단계 요청: id/pw + 기기정보. 비밀번호는 이 요청에서만 전송한다. 필드 누락은 서비스에서 검증한다. */
public record LoginRequest(@Nullable String userId, @Nullable String password, @Nullable DeviceRequest device) {
}
