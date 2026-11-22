package com.app.demo.user.application;

import com.app.demo.global.exception.BusinessException;
import com.app.demo.global.security.AuthUser;
import com.app.demo.user.domain.UserInfoRepository;
import com.app.demo.user.exception.UserErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/** 등록 기기의 푸시 토큰 관리 유스케이스. */
@Service
@RequiredArgsConstructor
public class DeviceService {

	private static final int MAX_PUSH_TOKEN_LENGTH = 255;

	private final UserInfoRepository infos;
	private final Clock clock;

	/** 토큰의 deviceId 와 일치하는 "현재 등록 기기"의 값만 갱신한다. 보낸 필드만 반영. */
	public void updatePush(AuthUser user, @Nullable String expoPushToken) {
		String deviceId = user.deviceId();
		if (deviceId == null) {
			throw new BusinessException(UserErrorCode.DEVICE_NOT_IN_TOKEN);
		}
		if (expoPushToken == null || expoPushToken.isBlank()) {
			return; // 갱신할 값 없음
		}
		String token = expoPushToken.trim();
		if (!isExpoPushToken(token)) {
			throw new BusinessException(UserErrorCode.PUSH_TOKEN_INVALID);
		}
		if (!infos.updatePush(user.userId(), deviceId, token, LocalDateTime.now(clock))) {
			throw new BusinessException(UserErrorCode.DEVICE_NOT_FOUND);
		}
	}

	/** Expo 푸시 토큰 형식인지 (임의 문자열 저장 방지) */
	private static boolean isExpoPushToken(String token) {
		boolean expoPrefix = token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken[");
		return expoPrefix && token.length() <= MAX_PUSH_TOKEN_LENGTH;
	}
}
