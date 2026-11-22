package com.app.demo.auth.domain;

import java.time.Duration;
import java.util.Optional;

/**
 * 로그인 세션 저장소 포트 (구현: Redis). 저장소에 접근할 수 없으면 구현체가 예외를 던지고, 호출자는 인증을 거부한다(fail-closed).
 */
public interface SessionStore {

	/** 세션을 저장한다. 같은 사용자의 기존 세션은 한 번의 쓰기로 덮어써진다(원자적 교체). */
	void save(String userId, Session session, Duration ttl);

	/**
	 * 세션을 조회하면서 만료를 ttl 뒤로 늦춘다(무활동 타임아웃 연장). 저장소 왕복 1번으로 처리한다.
	 * 세션이 없으면 empty 이고 아무것도 만들지 않는다.
	 */
	Optional<Session> findAndExtend(String userId, Duration ttl);

	void delete(String userId);
}
