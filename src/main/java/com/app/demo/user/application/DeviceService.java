package com.app.demo.user.application;

import com.app.demo.global.exception.BusinessException;
import com.app.demo.global.exception.ErrorCode;
import com.app.demo.global.security.AuthUser;
import com.app.demo.user.domain.UserInfo;
import com.app.demo.user.domain.UserInfoRepository;
import com.app.demo.user.domain.policy.DevicePolicy;
import com.app.demo.user.exception.UserErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/** 등록 기기의 푸시 토큰 관리 유스케이스. */
@Service
@RequiredArgsConstructor
public class DeviceService {

	private final UserInfoRepository infos;
	private final Clock clock;

	/**
	 * 현재 등록 기기의 푸시 토큰을 갱신한다. 보낸 필드만 반영.
	 * Access-Token 이 이 등록 기기에서 발급된 것이고(기기 ID 해시 일치), 기기 비밀키도 맞아야 한다.
	 * 기기 ID 는 비밀이 아니므로 ID 만으로는 다른 사람의 푸시 토큰을 덮어쓸 수 없게 한다.
	 */
	public void updatePush(AuthUser user, String deviceSecret, @Nullable String expoPushToken) {
		// ── [1단계] 기기: 토큰에 기기 있음 → 등록 기기 일치(400 DEVICE_NOT_FOUND) → 기기 비밀키(400 DEVICE_NOT_VERIFIED) ──
		String tokenDeviceIdHash = user.getDeviceIdHash();
		if (tokenDeviceIdHash == null) {
			throw new BusinessException(UserErrorCode.DEVICE_NOT_IN_TOKEN);
		}
		UserInfo info = infos.findOrCreate(user.getUserId());
		Optional<ErrorCode> deviceRejection = DevicePolicy.checkRegisteredDevice(tokenDeviceIdHash, info, deviceSecret);
		if (deviceRejection.isPresent()) {
			throw new BusinessException(deviceRejection.orElseThrow());
		}
		// 등록 기기가 없으면 checkRegisteredDevice 가 DEVICE_NOT_FOUND 로 거절했다.
		String registeredDeviceId = Objects.requireNonNull(info.getDeviceId());

		// ── [2단계] 푸시 토큰: 보낸 값 없음 → 형식 → 저장 ──
		if (expoPushToken == null || expoPushToken.isBlank()) {
			return; // 갱신할 값 없음
		}
		String token = expoPushToken.trim();
		Optional<ErrorCode> tokenRejection = DevicePolicy.checkPushToken(token);
		if (tokenRejection.isPresent()) {
			throw new BusinessException(tokenRejection.orElseThrow());
		}
		LocalDateTime now = LocalDateTime.now(clock);
		if (!infos.updatePush(user.getUserId(), registeredDeviceId, token, now)) {
			throw new BusinessException(UserErrorCode.DEVICE_NOT_FOUND);
		}
	}
}
