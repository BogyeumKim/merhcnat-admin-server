package com.app.demo.device.presentation;

import com.app.demo.auth.domain.AuthUser;
import com.app.demo.device.application.DeviceService;
import com.app.demo.global.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 기기 API (Bearer Access-Token 필요) */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

	private final DeviceService deviceService;

	public DeviceController(DeviceService deviceService) {
		this.deviceService = deviceService;
	}

	/** 로그인 완료 후 현재 기기의 푸시 토큰을 갱신한다. 성공 시 200 */
	@PutMapping("/me")
	public ApiResponse<Void> update(@AuthenticationPrincipal AuthUser user, @RequestBody DeviceUpdateRequest request) {
		deviceService.updatePush(user, request.expoPushToken());
		return ApiResponse.ok();
	}
}
