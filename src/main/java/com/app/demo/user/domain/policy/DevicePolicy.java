package com.app.demo.user.domain.policy;

import com.app.demo.global.exception.ErrorCode;
import com.app.demo.global.security.Sha256;
import com.app.demo.user.domain.UserInfo;
import com.app.demo.user.exception.UserErrorCode;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * 푸시 토큰을 갱신할 수 있는 등록 기기인지 가른다. 등록 기기 일치(400 DEVICE_NOT_FOUND) → 기기 비밀키(400 DEVICE_NOT_VERIFIED) 순으로 처음 걸린 사유.
 * 기기 ID 는 비밀이 아니므로 ID 만으로는 다른 사람의 푸시 토큰을 덮어쓸 수 없게 한다.
 */
@UtilityClass
public class DevicePolicy {

	private static final int MAX_PUSH_TOKEN_LENGTH = 255;

	/** ex) 토큰의 기기가 지금 등록 기기가 아님 → {@code DEVICE_NOT_FOUND}, 비밀키가 틀림 → {@code DEVICE_NOT_VERIFIED} */
	public Optional<ErrorCode> checkRegisteredDevice(String tokenDeviceIdHash, UserInfo info, String deviceSecret) {
		String registeredDeviceId = info.getDeviceId();
		if (registeredDeviceId == null) {
			return Optional.of(UserErrorCode.DEVICE_NOT_FOUND);
		}
		String registeredDeviceIdHash = Sha256.hex(registeredDeviceId);
		if (!registeredDeviceIdHash.equals(tokenDeviceIdHash)) {
			return Optional.of(UserErrorCode.DEVICE_NOT_FOUND);
		}
		if (!info.isTrustedDevice(registeredDeviceId, deviceSecret)) {
			return Optional.of(UserErrorCode.DEVICE_NOT_VERIFIED);
		}
		return Optional.empty();
	}

	/** Expo 푸시 토큰 형식인지 (임의 문자열 저장 방지). ex) {@code ExponentPushToken[xxx]} → empty, {@code abc} → {@code PUSH_TOKEN_INVALID} */
	public Optional<ErrorCode> checkPushToken(String token) {
		boolean expoPrefix = token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken[");
		if (!expoPrefix || token.length() > MAX_PUSH_TOKEN_LENGTH) {
			return Optional.of(UserErrorCode.PUSH_TOKEN_INVALID);
		}
		return Optional.empty();
	}
}
