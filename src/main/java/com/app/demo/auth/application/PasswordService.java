package com.app.demo.auth.application;

import com.app.demo.auth.domain.AccessToken;
import com.app.demo.auth.domain.PasswordPolicy;
import com.app.demo.auth.domain.TokenProvider;
import com.app.demo.auth.exception.AuthErrorCode;
import com.app.demo.global.api.ErrorData;
import com.app.demo.global.exception.BusinessException;
import com.app.demo.global.exception.CommonErrorCode;
import com.app.demo.global.security.AuthUser;
import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 비밀번호 변경 유스케이스 (로그인 완료 후 Bearer 필요).
 * 잘못된 현재 비밀번호는 로그인과 같은 오류 횟수에 합산해 5회에 영구 잠금한다(탈취 토큰으로 현재 비밀번호를 알아내는 시도 방어).
 * 규칙 위반/재사용은 횟수에 합산하지 않는다. 로그인과 마찬가지로 의도적으로 @Transactional 을 쓰지 않는다(실패 카운트 롤백 방지).
 * 변경에 성공하면 같은 기기로 새 Access-Token 을 발급해 로그인 세션을 교체한다(OWASP: 비밀번호 변경 시 세션 재발급).
 * 이전 토큰은 즉시 SESSION_REPLACED 가 되고, 기기 등록·기기 비밀키는 그대로 둔다.
 */
@Service
@RequiredArgsConstructor
public class PasswordService {

	private final UserRepository users;
	private final AccountLockService accountLock;
	private final SessionService sessions;
	private final TokenProvider tokens;
	private final Clock clock;

	/** 새 비밀번호 검증: 규칙 위반(400) / 재사용 금지(409). 실패 횟수에는 합산하지 않는다. */
	public void checkNewPassword(User user, String newPassword) {
		List<String> violations = PasswordPolicy.violations(user.userId(), newPassword);
		if (!violations.isEmpty()) {
			throw new BusinessException(AuthErrorCode.PASSWORD_POLICY_VIOLATION, ErrorData.violations(violations));
		}
		// 재사용 금지: 현재 비밀번호 + 이전 비밀번호 3개(user.old_password1~3)
		if (user.passwordReused(newPassword)) {
			throw new BusinessException(AuthErrorCode.PASSWORD_REUSED);
		}
	}

	/**
	 * 로그인 사용자의 비밀번호 변경: 현재 비밀번호 확인 → 새 비밀번호 규칙/재사용 검사 → 저장 → 오류 횟수 초기화 → 세션 교체.
	 *
	 * @return 교체된 세션의 새 Access-Token (응답의 Access-Token 헤더로 내려간다)
	 */
	public AccessToken change(AuthUser auth, String currentPassword, String newPassword) {
		String deviceIdHash = requireDeviceBinding(auth);
		User user = users.findById(auth.userId())
				.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));
		accountLock.verifySecret(user.userId(), () -> user.passwordMatches(currentPassword),
				remaining -> new BusinessException(AuthErrorCode.CURRENT_PASSWORD_INVALID, ErrorData.remaining(remaining)));
		checkNewPassword(user, newPassword);
		users.changePassword(user.userId(), newPassword, LocalDateTime.now(clock));
		users.resetFailCount(user.userId()); // 현재 비밀번호로 본인 확인이 끝났으므로 누적 오류 횟수를 초기화한다
		return reissueSession(user, deviceIdHash);
	}

	/** 같은 기기로 새 토큰을 발급하고 세션을 교체한다. 이전 토큰(혹시 남은 다른 토큰 포함)은 즉시 무효가 된다. */
	private AccessToken reissueSession(User user, String deviceIdHash) {
		AccessToken token = tokens.createAccessToken(user.userId(), user.userType(), deviceIdHash);
		sessions.start(user.userId(), token, deviceIdHash);
		return token;
	}

	/** 새 토큰도 같은 기기에 묶어야 하므로 기기 해시가 없는 토큰은 거부한다(변경 전에 확인). */
	private static String requireDeviceBinding(AuthUser auth) {
		String deviceIdHash = auth.deviceIdHash();
		if (deviceIdHash == null) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
		}
		return deviceIdHash;
	}
}
