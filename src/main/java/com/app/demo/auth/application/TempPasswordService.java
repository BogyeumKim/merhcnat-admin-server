package com.app.demo.auth.application;

import com.app.demo.auth.exception.AuthErrorCode;
import com.app.demo.global.api.ErrorData;
import com.app.demo.global.exception.BusinessException;
import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 임시비밀번호 변경: 비밀번호 변경 챌린지 토큰(일회용) + 현재(임시) 비밀번호 + 새 비밀번호 → 성공하면 로그인 완료.
 * 현재 비밀번호 오류는 로그인과 같은 횟수에 합산해 한도에서 잠근다(무차별 대입 방어).
 * 실패 횟수가 롤백되면 안 되므로 의도적으로 {@code @Transactional} 을 쓰지 않는다.
 */
@Service
@RequiredArgsConstructor
public class TempPasswordService {

	private final UserRepository users;
	private final AccountLockService accountLock;
	private final ChallengeService challenges;
	private final PasswordService passwordService;
	private final LoginCompletion completion;
	private final Clock clock;

	public LoginSuccess changeTempPassword(TempPasswordChangeCommand cmd) {
		Challenge ch = challenges.loadPasswordChange(cmd.challengeToken());
		User user = ch.user();
		if (!user.tempPassword()) {
			throw new BusinessException(AuthErrorCode.CHALLENGE_EXPIRED); // 이미 변경을 마쳐 임시비밀번호 상태가 아님
		}
		checkCurrentPassword(user, cmd.currentPassword());
		passwordService.checkNewPassword(user, cmd.newPassword());

		challenges.consume(ch); // 동시 요청 방지: 소진에 성공한 요청만 변경한다
		users.changePassword(user.userId(), cmd.newPassword(), LocalDateTime.now(clock));
		User changed = users.findById(user.userId()).orElse(user);
		return completion.completeVerified(changed, ch.claims().device());
	}

	private void checkCurrentPassword(User user, String currentPassword) {
		if (!user.passwordMatches(currentPassword)) {
			throw accountLock.recordFailure(user.userId(),
					remaining -> new BusinessException(AuthErrorCode.CURRENT_PASSWORD_INVALID, ErrorData.remaining(remaining)));
		}
	}
}
