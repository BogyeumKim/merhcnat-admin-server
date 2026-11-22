package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.LoginCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * 1단계 요청: id/pw + 기기정보. 비밀번호는 이 요청에서만 전송한다.
 * device 누락은 앱이 분기하는 DEVICE_REQUIRED 코드로 응답해야 하므로 Bean Validation 대신 DeviceInfo 생성 시 검증한다.
 */
public record LoginRequest(
		@NotBlank @Size(max = 30) String userId,
		@NotBlank @Size(max = 64) String password,
		@Valid @Nullable DeviceRequest device) {

	public LoginCommand toCommand() {
		DeviceRequest deviceOrMissing = Objects.requireNonNullElse(device, DeviceRequest.MISSING);
		// 아이디 앞뒤 공백은 같은 계정으로 본다(복사·붙여넣기 실수 방지)
		return new LoginCommand(userId.strip(), password, deviceOrMissing.toDomain(), deviceOrMissing.deviceSecret());
	}
}
