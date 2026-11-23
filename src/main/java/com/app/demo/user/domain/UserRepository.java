package com.app.demo.user.domain;

import java.time.LocalDateTime;
import java.util.Optional;

/** 사용자(user 테이블) 저장소 포트. 구현은 infrastructure 에 있다. */
public interface UserRepository {

	Optional<User> findById(String userId);

	/**
	 * 비밀 값(비밀번호·OTP) 비교 1회를 원자적으로 예약한다: 오류 횟수가 max 미만일 때만 +1 한다.
	 * 동시 요청이 몰려도 예약에 성공한 요청만 비교하므로 비교 횟수는 max 를 넘지 않는다.
	 *
	 * @return 예약에 성공하면 true, 이미 한도에 도달했으면 false
	 */
	boolean reserveAttempt(String userId, int max);

	/** 비교에 성공한 시도의 예약을 되돌린다(0 미만으로 내려가지 않음). */
	void refundAttempt(String userId);

	/** 현재 오류 횟수 (계정이 없으면 0) */
	int failCount(String userId);

	/** 최종 로그인 성공 시 오류 횟수 초기화 */
	void resetFailCount(String userId);

	/** 비밀번호 변경: old_password1~3 을 한 칸씩 밀고 현재 값을 old_password1 에 넣은 뒤 새 값({@link PasswordHash})/변경 시각을 저장한다 (UPDATE 한 문장). */
	void changePassword(String userId, String newPassword, LocalDateTime now);
}
