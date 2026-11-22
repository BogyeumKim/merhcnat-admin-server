package com.app.demo.auth.application;

import com.app.demo.user.domain.DeviceInfo;

/** 1단계 로그인 요청. device 는 생성 시점에 이미 검증된 값 객체다. */
public record LoginCommand(String userId, String password, DeviceInfo device) {
}
