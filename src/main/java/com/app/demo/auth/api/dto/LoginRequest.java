package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.LoginCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * 1단계 요청: id/pw + 기기정보. 비밀번호는 이 요청에서만 전송한다.
 * device 누락은 앱이 분기하는 DEVICE_REQUIRED 코드로 응답해야 하므로 Bean Validation 대신 DeviceInfo 생성 시 검증한다.
 */
public record LoginRequest(
		@NotBlank String userId,
		@NotBlank String password,
		@Valid @Nullable DeviceRequest device) {

	public LoginCommand toCommand() {
		DeviceRequest deviceOrMissing = Objects.requireNonNullElse(device, DeviceRequest.MISSING);
		return new LoginCommand(userId, password, deviceOrMissing.toDomain());
	}
}
