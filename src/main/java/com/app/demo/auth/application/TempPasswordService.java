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
 * 비밀번호 변경 단계(임시비밀번호 또는 만료된 비밀번호): 비밀번호 변경 챌린지 토큰(일회용) + 현재 비밀번호 + 새 비밀번호 → 성공하면 로그인 완료.
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
	private final PasswordChangePolicy passwordChangePolicy;
	private final Clock clock;

	public LoginStep.Completed changeTempPassword(TempPasswordChangeCommand cmd) {
		// ── [1단계] 챌린지: 서명·만료 → 잠금·이용 가능 → 아직 변경이 필요한 상태 ──
		Challenge ch = challenges.loadPasswordChange(cmd.getChallengeToken());
		User user = ch.getUser();
		if (!passwordChangePolicy.isRequired(user)) {
			throw new BusinessException(AuthErrorCode.CHALLENGE_EXPIRED); // 이미 변경을 마쳐 변경이 필요한 상태가 아님
		}

		// ── [2단계] 비밀번호: 현재 비밀번호(오류 합산) → 새 비밀번호 규칙·재사용 ──
		checkCurrentPassword(user, cmd.getCurrentPassword());
		passwordService.checkNewPassword(user, cmd.getNewPassword());

		// ── [3단계] 변경: 챌린지 소진 → 저장 → 로그인 완료 ──
		challenges.consume(ch); // 동시 요청 방지: 소진에 성공한 요청만 변경한다
		users.changePassword(user.getUserId(), cmd.getNewPassword(), LocalDateTime.now(clock));
		User changed = users.findById(user.getUserId()).orElse(user);
		return completion.completeVerified(changed, ch.getClaims().getDevice());
	}

	private void checkCurrentPassword(User user, String currentPassword) {
		accountLock.verifySecret(user.getUserId(), () -> user.passwordMatches(currentPassword),
				remaining -> new BusinessException(AuthErrorCode.CURRENT_PASSWORD_INVALID, ErrorData.remaining(remaining)));
	}
}
