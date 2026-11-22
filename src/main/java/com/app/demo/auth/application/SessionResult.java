package com.app.demo.auth.application;

import com.app.demo.user.domain.DeviceState;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/** 로그인된 세션 정보 (비밀번호 만료로 변경이 필요한지 포함) */
public record SessionResult(String userId, @Nullable Integer loginType, @Nullable Integer userType, DeviceState deviceState,
		boolean passwordChangeRequired, @Nullable LocalDateTime passwordChangedAt) {
}
