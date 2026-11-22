package com.app.demo.auth.application;

import com.app.demo.user.domain.DeviceState;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/**
 * 로그인된 세션 정보.
 *
 * @param deviceSecret 이번 로그인으로 새로 발급한 기기 비밀키(원문). 앱이 SecureStore 에 보관한다. 새로 발급하지 않았으면 null
 */
public record SessionResult(String userId, @Nullable Integer loginType, @Nullable Integer userType, DeviceState deviceState,
		boolean passwordChangeRequired, @Nullable LocalDateTime passwordChangedAt, @Nullable String deviceSecret) {
}
