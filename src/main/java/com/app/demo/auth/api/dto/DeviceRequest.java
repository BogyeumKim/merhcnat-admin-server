package com.app.demo.auth.api.dto;

import com.app.demo.user.domain.DeviceInfo;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.jspecify.annotations.Nullable;

/**
 * 앱이 보내는 기기 정보. deviceId 는 앱이 최초 실행 때 만든 UUID. 여기서는 과도한 길이만 막는다.
 * 필수·형식 검증은 앱이 분기하는 DEVICE_REQUIRED / DEVICE_INVALID 코드를 유지하기 위해 {@link DeviceInfo} 가 생성 시점에 한다.
 */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class DeviceRequest {

	/** 기기 정보를 아예 보내지 않은 요청. 도메인 변환 시 DEVICE_REQUIRED 로 거부된다. */
	static final DeviceRequest MISSING = DeviceRequest.with().create();

	@Size(max = 64)
	private final @Nullable String deviceId;

	@Size(max = 10)
	private final @Nullable String platform;

	@Size(max = 100)
	private final @Nullable String osVersion;

	@Size(max = 100)
	private final @Nullable String deviceModel;

	@Size(max = 100)
	private final @Nullable String appVersion;

	/** 이전 로그인 완료 때 받은 기기 비밀키. 등록 기기에서 OTP 를 생략하려면 필요하다 */
	@Size(max = 128)
	private final @Nullable String deviceSecret;

	/** 도메인 값 객체로 변환. 누락된 필수값은 빈 문자열로 넘겨 {@link DeviceInfo} 가 거부하게 한다. */
	public DeviceInfo toDomain() {
		String requiredDeviceId = Objects.toString(deviceId, "");
		String requiredPlatform = Objects.toString(platform, "");
		return DeviceInfo.of(requiredDeviceId, requiredPlatform, osVersion, deviceModel, appVersion);
	}
}
