package com.app.demo.device.application;

import com.app.demo.auth.domain.AuthUser;
import com.app.demo.global.exception.BusinessException;
import com.app.demo.user.domain.UserInfoRepository;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 등록 기기의 푸시 토큰 관리 유스케이스. */
@Service
public class DeviceService {

	private final UserInfoRepository infos;

	public DeviceService(UserInfoRepository infos) {
		this.infos = infos;
	}

	/** 토큰의 deviceId 와 일치하는 "현재 등록 기기"의 값만 갱신한다. 보낸 필드만 반영. */
	public void updatePush(AuthUser user, @Nullable String expoPushToken) {
		String deviceId = user.deviceId();
		if (deviceId == null) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "DEVICE_REQUIRED", "기기 정보가 없는 토큰입니다.");
		}
		String token = expoPushToken == null ? null : expoPushToken.trim();
		if (token != null && token.isEmpty()) {
			token = null;
		}
		// Expo 푸시 토큰 형식 검증 (임의 문자열 저장 방지)
		if (token != null && !((token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken[")) && token.length() <= 255)) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "PUSH_TOKEN_INVALID", "올바르지 않은 푸시 토큰입니다.");
		}
		if (token == null) {
			return; // 갱신할 값 없음
		}
		if (!infos.updatePush(user.userId(), deviceId, token, LocalDateTime.now())) {
			throw new BusinessException(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND", "등록된 기기가 아닙니다.");
		}
	}
}
