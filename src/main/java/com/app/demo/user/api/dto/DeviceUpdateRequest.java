package com.app.demo.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * 푸시 토큰 갱신 요청. 보낸 필드만 갱신한다.
 *
 * @param deviceSecret 로그인 완료 때 받은 기기 비밀키. 이 기기가 실제 등록 기기임을 증명한다
 */
public record DeviceUpdateRequest(
		@NotBlank @Size(max = 128) String deviceSecret,
		@Nullable @Size(max = 255) String expoPushToken) {
}
