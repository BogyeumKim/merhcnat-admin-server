package com.app.demo.auth.application;

import com.app.demo.user.domain.DeviceState;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/** 로그인된 세션 정보. */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SessionResult {

	private final String userId;

	private final @Nullable Integer loginType;

	private final @Nullable Integer userType;

	private final DeviceState deviceState;

	private final boolean passwordChangeRequired;

	private final @Nullable LocalDateTime passwordChangedAt;

	/** 이번 로그인으로 새로 발급한 기기 비밀키(원문). 앱이 SecureStore 에 보관한다. 새로 발급하지 않았으면 null */
	private final @Nullable String deviceSecret;
}
