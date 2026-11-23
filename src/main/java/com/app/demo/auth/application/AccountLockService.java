package com.app.demo.auth.application;

import com.app.demo.auth.config.AuthProperties;
import com.app.demo.auth.domain.IdMasking;
import com.app.demo.auth.domain.policy.AccountPolicy;
import com.app.demo.auth.exception.AuthErrorCode;
import com.app.demo.global.api.ErrorData;
import com.app.demo.global.exception.BusinessException;
import com.app.demo.global.exception.ErrorCode;
import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserInfoRepository;
import com.app.demo.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * 계정 상태 검사와 잠금 정책.
 * id/pw·OTP·현재 비밀번호 오류를 하나의 횟수로 합산하고, 한도에 도달하면 423 ACCOUNT_LOCKED 로 차단한다(관리자 해제).
 *
 * <p>비밀 값 비교는 "예약 → 비교 → 성공 시 환불" 순서로 한다. 비교하기 전에 오류 횟수를 원자적으로 +1 해 두므로
 * 동시 요청이 몰려도 한도(max)를 넘는 비교는 일어나지 않는다.
 *
 * <p>실패 횟수 증가는 예외가 던져져도 반드시 남아야 하므로 의도적으로 {@code @Transactional} 을 쓰지 않는다.
 */
@Service
@RequiredArgsConstructor
public class AccountLockService {

	private final UserRepository users;
	private final UserInfoRepository infos;
	private final AuthProperties authProps;
	private final Clock clock;

	/**
	 * 로그인 1단계의 비밀번호 확인. 아이디 존재·잠금 여부를 숨기기 위해 비밀번호가 틀리면 항상 같은
	 * 401 INVALID_CREDENTIALS(data 없음)를 던지고, 잠금(423)은 비밀번호가 맞았을 때만 알려 준다.
	 */
	public void verifyLoginPassword(User user, String password) {
		String userId = user.getUserId();
		if (!users.reserveAttempt(userId, maxFailCount())) {
			// 이미 잠긴 계정: 횟수는 더 세지 않고, 비밀번호가 맞을 때만 잠금 사실을 알려 준다
			throw user.passwordMatches(password) ? alreadyLocked(userId) : invalidCredentials();
		}
		if (user.passwordMatches(password)) {
			users.refundAttempt(userId);
			return;
		}
		if (remainingAttempts(userId) <= 0) {
			lockNow(userId); // 잠그되 응답은 다른 실패와 똑같이 401
		}
		throw invalidCredentials();
	}

	/**
	 * 비밀번호를 이미 확인한 단계(OTP·현재 비밀번호)의 비밀 값 확인. 틀리면 남은 횟수를 담은 호출자의 예외를,
	 * 한도에 도달하면 423 을 던진다.
	 */
	public void verifySecret(String userId, BooleanSupplier matches, IntFunction<BusinessException> onFailure) {
		verifySecret(userId, () -> matches.getAsBoolean() ? Optional.of(Boolean.TRUE) : Optional.empty(), onFailure);
	}

	/** {@link #verifySecret(String, BooleanSupplier, IntFunction)} 와 같되, 일치했을 때 찾은 값(예: TOTP time-step)을 돌려준다. */
	public <T> T verifySecret(String userId, Supplier<Optional<T>> match, IntFunction<BusinessException> onFailure) {
		if (!users.reserveAttempt(userId, maxFailCount())) {
			throw alreadyLocked(userId);
		}
		Optional<T> matched = match.get();
		if (matched.isPresent()) {
			users.refundAttempt(userId);
			return matched.get();
		}
		int remaining = remainingAttempts(userId);
		if (remaining <= 0) {
			throw lockNow(userId);
		}
		throw onFailure.apply(remaining);
	}

	/** 이미 잠긴 계정이면 423 (챌린지 단계 진입 시 확인. 비밀번호를 이미 확인한 뒤라 잠금 사실을 알려도 된다) */
	public void checkLocked(User user) {
		if (user.isLocked(maxFailCount())) {
			throw alreadyLocked(user.getUserId());
		}
	}

	/** 정지(403 ACCOUNT_SUSPENDED)/삭제(없는 계정과 같은 401) 이용자 차단. 실패 횟수에는 합산하지 않는다. */
	public void checkUsable(User user) {
		Optional<ErrorCode> rejection = AccountPolicy.checkUsable(user);
		if (rejection.isPresent()) {
			throw new BusinessException(rejection.orElseThrow());
		}
	}

	/** 아이디가 없거나 비밀번호가 틀린 경우의 공통 응답. 계정 존재를 드러내지 않도록 data 를 싣지 않는다. */
	public static BusinessException invalidCredentials() {
		return new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
	}

	private int maxFailCount() {
		return authProps.getMaxFailCount();
	}

	private int remainingAttempts(String userId) {
		return maxFailCount() - users.failCount(userId);
	}

	/** 한도에 도달한 시각을 기록하고 423 예외를 만든다. */
	private BusinessException lockNow(String userId) {
		LocalDateTime now = LocalDateTime.now(clock).withNano(0);
		infos.markLocked(userId, now);
		return lockedError(userId, now);
	}

	private BusinessException alreadyLocked(String userId) {
		return lockedError(userId, infos.findOrCreate(userId).getLockedAt());
	}

	/** data 에 마스킹된 아이디와 잠긴 시각(ISO-8601 + 오프셋, 기록이 없으면 null)을 담는다. */
	private BusinessException lockedError(String userId, @Nullable LocalDateTime lockedAt) {
		String iso = lockedAt == null ? null
				: lockedAt.atZone(clock.getZone()).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		return new BusinessException(AuthErrorCode.ACCOUNT_LOCKED, ErrorData.locked(IdMasking.mask(userId), iso));
	}
}
