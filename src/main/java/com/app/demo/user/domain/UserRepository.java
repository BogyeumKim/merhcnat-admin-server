package com.app.demo.user.domain;

import java.util.Optional;

/** 사용자(user 테이블) 저장소 포트. 구현은 infrastructure 에 있다. */
public interface UserRepository {

	Optional<User> findById(String userId);

	/** 오류 횟수를 DB 에서 원자적으로 +1 하고 증가 후 값을 반환한다. */
	int increaseFailCount(String userId);

	/** 최종 로그인 성공 시 오류 횟수 초기화 */
	void resetFailCount(String userId);
}
