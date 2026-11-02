package com.app.demo.auth.presentation;

import org.jspecify.annotations.Nullable;

/** OTP 발송 요청 (loginType=1). 비밀번호 대신 1단계에서 받은 challengeToken 을 쓴다. channel: KAKAO | SMS */
public record OtpSendRequest(@Nullable String challengeToken, @Nullable String channel) {
}
