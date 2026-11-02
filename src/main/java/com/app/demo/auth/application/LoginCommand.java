package com.app.demo.auth.application;

import com.app.demo.user.domain.DeviceInfo;
import org.jspecify.annotations.Nullable;

/** 1단계 로그인 요청. device 는 필수지만 입력 검증을 서비스에서 하므로 null 을 허용한다. */
public record LoginCommand(String userId, String password, @Nullable DeviceInfo device) {
}
