package com.app.demo.auth.application;

import com.app.demo.user.domain.DeviceInfo;
import org.jspecify.annotations.Nullable;

/**
 * 1단계 로그인 요청. device 는 생성 시점에 이미 검증된 값 객체다.
 *
 * @param deviceSecret 등록 기기임을 증명하는 비밀키(이전 로그인 완료 때 발급). 없으면 OTP 를 생략하지 않는다
 */
public record LoginCommand(String userId, String password, DeviceInfo device, @Nullable String deviceSecret) {
}
