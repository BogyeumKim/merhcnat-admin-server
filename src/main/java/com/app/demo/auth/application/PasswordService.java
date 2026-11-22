package com.app.demo.auth.application;

import com.app.demo.auth.domain.PasswordPolicy;
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
 */
@Service
@RequiredArgsConstructor
public class PasswordService {

	private final UserRepository users;
	private final AccountLockService accountLock;
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

	/** 로그인 사용자의 비밀번호 변경: 잠금 확인 → 현재 비밀번호 확인 → 새 비밀번호 규칙/재사용 검사 → 저장 */
	public void change(AuthUser auth, String currentPassword, String newPassword) {
		User user = users.findById(auth.userId())
				.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));
		accountLock.checkLocked(user);
		if (!user.passwordMatches(currentPassword)) {
			throw accountLock.recordFailure(user.userId(),
					remaining -> new BusinessException(AuthErrorCode.CURRENT_PASSWORD_INVALID, ErrorData.remaining(remaining)));
		}
		checkNewPassword(user, newPassword);
		users.changePassword(user.userId(), newPassword, LocalDateTime.now(clock));
	}
}
