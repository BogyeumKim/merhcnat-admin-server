package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.LoginCommand;
import com.app.demo.user.domain.DeviceInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.jspecify.annotations.Nullable;

/**
 * 1단계 요청: id/pw + 기기정보. 비밀번호는 이 요청에서만 전송한다.
 * device 누락은 앱이 분기하는 DEVICE_REQUIRED 코드로 응답해야 하므로 Bean Validation 대신 DeviceInfo 생성 시 검증한다.
 */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class LoginRequest {

	@NotBlank
	@Size(max = 30)
	private final String userId;

	@NotBlank
	@Size(max = 64)
	private final String password;

	@Valid
	private final @Nullable DeviceRequest device;

	public LoginCommand toCommand() {
		DeviceRequest deviceOrMissing = Objects.requireNonNullElse(device, DeviceRequest.MISSING);
		DeviceInfo deviceInfo = deviceOrMissing.toDomain();
		// 아이디 앞뒤 공백은 같은 계정으로 본다(복사·붙여넣기 실수 방지)
		String strippedUserId = userId.strip();
		return LoginCommand.with()
				.userId(strippedUserId)
				.password(password)
				.device(deviceInfo)
				.deviceSecret(deviceOrMissing.getDeviceSecret())
				.create();
	}
}
