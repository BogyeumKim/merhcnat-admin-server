package com.app.demo.auth.domain;

import com.app.demo.user.domain.DeviceInfo;

/** MFA 챌린지 토큰에서 복원한 "진행 중인 로그인" 정보. */
public record MfaClaims(String userId, String jti, DeviceInfo device) {
}
