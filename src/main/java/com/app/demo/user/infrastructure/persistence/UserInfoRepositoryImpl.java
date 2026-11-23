package com.app.demo.user.infrastructure.persistence;

import com.app.demo.user.domain.DeviceInfo;
import com.app.demo.user.domain.UserInfo;
import com.app.demo.user.domain.UserInfoRepository;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

/** {@link UserInfoRepository} 의 MyBatis-Plus 구현. */
@Repository
@RequiredArgsConstructor
class UserInfoRepositoryImpl implements UserInfoRepository {

	private final UserInfoMapper mapper;

	@Override
	public UserInfo findOrCreate(String userId) {
		UserInfoEntity e = mapper.selectById(userId);
		if (e == null) {
			UserInfoEntity created = new UserInfoEntity();
			created.setUserId(userId);
			try {
				mapper.insert(created); // null 필드는 건너뛰어 DB 기본값 적용
			} catch (DuplicateKeyException _) {
				// 동시 생성 경합: 이미 만들어졌으므로 아래에서 다시 조회
			}
			e = mapper.selectById(userId);
		}
		return toDomain(e);
	}

	@Override
	public boolean existsByDeviceId(String deviceId) {
		return mapper.selectCount(new QueryWrapper<UserInfoEntity>().eq("device_id", deviceId)) > 0;
	}

	@Override
	public @Nullable String findUserIdByDeviceId(String deviceId) {
		UserInfoEntity e = mapper.selectOne(new QueryWrapper<UserInfoEntity>().eq("device_id", deviceId).last("limit 1"));
		return e == null ? null : e.getUserId();
	}

	@Override
	public void markLocked(String userId, LocalDateTime at) {
		mapper.update(null, new UpdateWrapper<UserInfoEntity>().eq("user_id", userId).set("locked_at", at));
	}

	@Override
	public void issueChallenge(String userId, String jti, LocalDateTime expiresAt) {
		mapper.update(null, new UpdateWrapper<UserInfoEntity>().eq("user_id", userId)
				.set("mfa_jti", jti).set("mfa_expires_at", expiresAt).set("otp_send_count", 0));
	}

	@Override
	public boolean consumeMfa(String userId, String jti) {
		return mapper.update(null, new UpdateWrapper<UserInfoEntity>()
				.eq("user_id", userId).eq("mfa_jti", jti).set("mfa_jti", null).set("mfa_expires_at", null)) == 1;
	}

	@Override
	public void saveOtp(String userId, String codeHash, String channel, LocalDateTime expiresAt, LocalDateTime sentAt) {
		// 사용자당 유효 OTP 1개: 덮어쓰기로 이전 코드는 자동 폐기
		mapper.update(null, new UpdateWrapper<UserInfoEntity>().eq("user_id", userId)
				.set("otp_code_hash", codeHash).set("otp_channel", channel)
				.set("otp_expires_at", expiresAt).set("otp_sent_at", sentAt));
	}

	@Override
	public boolean reserveOtpSend(String userId, String jti, String channel, int maxPerChallenge,
			LocalDateTime challengeExtendedUntil, LocalDateTime now) {
		return mapper.update(null, new UpdateWrapper<UserInfoEntity>()
				.setSql("otp_send_count = otp_send_count + 1")
				.set("otp_channel", channel).set("otp_sent_at", now).set("mfa_expires_at", challengeExtendedUntil)
				.eq("user_id", userId).eq("mfa_jti", jti)
				.lt("otp_send_count", maxPerChallenge)) == 1;
	}

	@Override
	public boolean consumeOtp(String userId, String codeHash) {
		return mapper.update(null, new UpdateWrapper<UserInfoEntity>()
				.eq("user_id", userId).eq("otp_code_hash", codeHash)
				.set("otp_code_hash", null).set("otp_channel", null).set("otp_expires_at", null)) == 1;
	}

	@Override
	public void updateLastTotpStep(String userId, long step) {
		mapper.update(null, new UpdateWrapper<UserInfoEntity>().eq("user_id", userId).set("last_totp_step", step));
	}

	@Override
	public void updateLastOtpChannel(String userId, String channel) {
		mapper.update(null, new UpdateWrapper<UserInfoEntity>().eq("user_id", userId).set("last_otp_channel", channel));
	}

	@Override
	public void markLogin(String userId, LocalDateTime at) {
		mapper.update(null, new UpdateWrapper<UserInfoEntity>().eq("user_id", userId).set("last_login_at", at));
	}

	@Override
	public void registerDevice(String userId, DeviceInfo d, @Nullable String deviceSecretHash, LocalDateTime now) {
		UserInfo current = findOrCreate(userId);
		boolean same = current.hasDevice(d.getDeviceId());
		if (!same) {
			// ① 이 기기를 쓰던 다른 사용자의 기기 정보 비우기 (이전 계정으로 오발송 방지)
			mapper.update(null, new UpdateWrapper<UserInfoEntity>().eq("device_id", d.getDeviceId()).ne("user_id", userId)
					.set("device_id", null).set("device_platform", null)
					.set("os_version", null).set("device_model", null).set("app_version", null)
					.set("expo_push_token", null).set("expo_push_token_updated_at", null).set("device_secret_hash", null));
		}
		// ② 이 사용자의 기기를 새 기기로 덮어쓰기
		UpdateWrapper<UserInfoEntity> w = new UpdateWrapper<UserInfoEntity>().eq("user_id", userId)
				.set("device_id", d.getDeviceId()).set("device_platform", d.getPlatform())
				.set("os_version", d.getOsVersion()).set("device_model", d.getDeviceModel()).set("app_version", d.getAppVersion());
		if (!same) {
			w.set("expo_push_token", null).set("expo_push_token_updated_at", null);
		}
		if (deviceSecretHash != null || !same) {
			w.set("device_secret_hash", deviceSecretHash); // 새 비밀키로 교체, 다른 기기로 바뀌었는데 새 키가 없으면 비움
		}
		mapper.update(null, w);
	}

	@Override
	public boolean updatePush(String userId, String deviceId, @Nullable String expoPushToken, LocalDateTime now) {
		UpdateWrapper<UserInfoEntity> w = new UpdateWrapper<UserInfoEntity>().eq("user_id", userId).eq("device_id", deviceId);
		if (expoPushToken != null) {
			// 같은 토큰이 다른 사용자 행에 남아 있으면 해제 (이전 소유자에게 오발송 방지)
			mapper.update(null, new UpdateWrapper<UserInfoEntity>().eq("expo_push_token", expoPushToken)
					.ne("user_id", userId).set("expo_push_token", null));
			w.set("expo_push_token", expoPushToken).set("expo_push_token_updated_at", now);
		}
		return mapper.update(null, w) == 1;
	}

	/** 엔티티 → 도메인 (인증 판단에 필요한 값만) */
	private static UserInfo toDomain(UserInfoEntity e) {
		int otpSendCount = zeroIfNull(e.getOtpSendCount());
		return UserInfo.with()
				.userId(e.getUserId())
				.deviceId(e.getDeviceId())
				.deviceSecretHash(e.getDeviceSecretHash())
				.lastTotpStep(e.getLastTotpStep())
				.mfaJti(e.getMfaJti())
				.mfaExpiresAt(e.getMfaExpiresAt())
				.otpCodeHash(e.getOtpCodeHash())
				.otpChannel(e.getOtpChannel())
				.otpExpiresAt(e.getOtpExpiresAt())
				.lockedAt(e.getLockedAt())
				.lastOtpChannel(e.getLastOtpChannel())
				.otpSendCount(otpSendCount)
				.create();
	}

	private static int zeroIfNull(@Nullable Integer value) {
		return value == null ? 0 : value;
	}
}
