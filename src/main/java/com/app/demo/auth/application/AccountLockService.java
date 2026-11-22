package com.app.demo.auth.application;

import com.app.demo.auth.config.AuthProperties;
import com.app.demo.auth.domain.IdMasking;
import com.app.demo.auth.exception.AuthErrorCode;
import com.app.demo.global.api.ErrorData;
import com.app.demo.global.exception.BusinessException;
import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserInfoRepository;
import com.app.demo.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.IntFunction;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * 계정 상태 검사와 잠금 정책.
 * id/pw·OTP·현재 비밀번호 오류를 하나의 횟수로 합산하고, 한도에 도달하면 423 ACCOUNT_LOCKED 로 차단한다(관리자 해제).
 * 실패 횟수 증가는 예외가 던져져도 반드시 남아야 하므로 의도적으로 {@code @Transactional} 을 쓰지 않는다.
 */
@Service
@RequiredArgsConstructor
public class AccountLockService {

	private final UserRepository users;
	private final UserInfoRepository infos;
	private final AuthProperties authProps;
	private final Clock clock;

	/** 이미 잠긴 계정이면 423 (올바른 비밀번호/OTP 여도 거부) */
	public void checkLocked(User user) {
		if (user.isLocked(authProps.maxFailCount())) {
			throw lockedError(user.userId(), infos.findOrCreate(user.userId()).lockedAt());
		}
	}

	/** 정지(403 ACCOUNT_SUSPENDED)/삭제(없는 계정과 같은 401) 이용자 차단. 실패 횟수에는 합산하지 않는다. */
	public void checkUsable(User user) {
		if (user.isDeleted()) {
			throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
		}
		if (user.isSuspended()) {
			throw new BusinessException(AuthErrorCode.ACCOUNT_SUSPENDED);
		}
	}

	/**
	 * 인증 실패 1회 기록(DB 원자적 증가).
	 * 한도에 도달한 바로 그 요청이면 잠금 시각을 남기고 423 을, 아니면 호출자가 만든 예외(남은 횟수 전달)를 반환한다.
	 */
	public BusinessException recordFailure(String userId, IntFunction<BusinessException> onRemaining) {
		int count = users.increaseFailCount(userId);
		int remaining = authProps.maxFailCount() - count;
		if (remaining > 0) {
			return onRemaining.apply(remaining);
		}
		LocalDateTime now = LocalDateTime.now(clock).withNano(0);
		infos.markLocked(userId, now);
		return lockedError(userId, now);
	}

	/** data 에 마스킹된 아이디와 잠긴 시각(ISO-8601 + 오프셋, 기록이 없으면 null)을 담는다. */
	private BusinessException lockedError(String userId, @Nullable LocalDateTime lockedAt) {
		String iso = lockedAt == null ? null
				: lockedAt.atZone(clock.getZone()).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		return new BusinessException(AuthErrorCode.ACCOUNT_LOCKED, ErrorData.locked(IdMasking.mask(userId), iso));
	}
}
