package com.app.demo.user.api;

import com.app.demo.global.security.AuthUser;
import com.app.demo.user.api.dto.DeviceUpdateRequest;
import com.app.demo.user.application.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 기기 API (Bearer Access-Token 필요) */
@RestController
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

	private final DeviceService deviceService;

	/** 로그인 완료 후 현재 기기의 푸시 토큰을 갱신한다. 성공 시 200 */
	@PutMapping(value = "/api/devices/me", consumes = MediaType.APPLICATION_JSON_VALUE)
	public void update(
			@AuthenticationPrincipal AuthUser user,
			@Valid @RequestBody DeviceUpdateRequest request) {

		log.info("Device push update received");

		deviceService.updatePush(user, request.getDeviceSecret(), request.getExpoPushToken());
	}
}
