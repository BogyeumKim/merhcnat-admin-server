package com.app.demo.auth.domain;

import org.jspecify.annotations.Nullable;

/** Access-Token 검증으로 확인된 인증 사용자. 컨트롤러에서 {@code @AuthenticationPrincipal} 로 받는다. */
public record AuthUser(String userId, @Nullable String deviceId) {
}
