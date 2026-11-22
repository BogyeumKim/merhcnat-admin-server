package com.app.demo.user.application;

import com.app.demo.global.exception.BusinessException;
import com.app.demo.global.security.AuthUser;
import com.app.demo.global.security.Sha256;
import com.app.demo.user.domain.UserInfo;
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

	/**
	 * 현재 등록 기기의 푸시 토큰을 갱신한다. 보낸 필드만 반영.
	 * Access-Token 이 이 등록 기기에서 발급된 것이고(기기 ID 해시 일치), 기기 비밀키도 맞아야 한다.
	 * 기기 ID 는 비밀이 아니므로 ID 만으로는 다른 사람의 푸시 토큰을 덮어쓸 수 없게 한다.
	 */
	public void updatePush(AuthUser user, String deviceSecret, @Nullable String expoPushToken) {
		String registeredDeviceId = requireVerifiedDevice(user, deviceSecret);
		if (expoPushToken == null || expoPushToken.isBlank()) {
			return; // 갱신할 값 없음
		}
		String token = expoPushToken.trim();
		if (!isExpoPushToken(token)) {
			throw new BusinessException(UserErrorCode.PUSH_TOKEN_INVALID);
		}
		if (!infos.updatePush(user.userId(), registeredDeviceId, token, LocalDateTime.now(clock))) {
			throw new BusinessException(UserErrorCode.DEVICE_NOT_FOUND);
		}
	}

	/** 토큰의 기기가 지금 등록된 기기인지(404), 기기 비밀키가 맞는지(403) 확인하고 등록 기기 ID 를 돌려준다. */
	private String requireVerifiedDevice(AuthUser user, String deviceSecret) {
		if (user.deviceIdHash() == null) {
			throw new BusinessException(UserErrorCode.DEVICE_NOT_IN_TOKEN);
		}
		UserInfo info = infos.findOrCreate(user.userId());
		String registeredDeviceId = info.deviceId();
		if (registeredDeviceId == null || !Sha256.hex(registeredDeviceId).equals(user.deviceIdHash())) {
			throw new BusinessException(UserErrorCode.DEVICE_NOT_FOUND);
		}
		if (!info.isTrustedDevice(registeredDeviceId, deviceSecret)) {
			throw new BusinessException(UserErrorCode.DEVICE_NOT_VERIFIED);
		}
		return registeredDeviceId;
	}

	/** Expo 푸시 토큰 형식인지 (임의 문자열 저장 방지) */
	private static boolean isExpoPushToken(String token) {
		boolean expoPrefix = token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken[");
		return expoPrefix && token.length() <= MAX_PUSH_TOKEN_LENGTH;
	}
}
