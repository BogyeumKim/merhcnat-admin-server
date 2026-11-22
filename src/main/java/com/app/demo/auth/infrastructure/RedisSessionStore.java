package com.app.demo.auth.infrastructure;

import com.app.demo.auth.domain.Session;
import com.app.demo.auth.domain.SessionStore;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Redis 세션 저장소. key = {@code auth:session:{userId}}, value = Session JSON.
 * 저장은 {@code SET key value EX ttl} 한 번이라 기존 세션이 원자적으로 교체되고,
 * 조회·연장은 {@code GETEX key EX ttl} 한 번이다(키가 없으면 아무것도 만들지 않는다).
 * Redis 에 접근할 수 없으면 Spring 의 DataAccessException 이 그대로 올라가고 호출자가 인증을 거부한다.
 */
@Component
@RequiredArgsConstructor
class RedisSessionStore implements SessionStore {

	private static final String KEY_PREFIX = "auth:session:";

	private final StringRedisTemplate redis;
	private final JsonMapper jsonMapper;

	@Override
	public void save(String userId, Session session, Duration ttl) {
		redis.opsForValue().set(key(userId), jsonMapper.writeValueAsString(session), ttl);
	}

	@Override
	public Optional<Session> findAndExtend(String userId, Duration ttl) {
		String json = redis.opsForValue().getAndExpire(key(userId), ttl);
		return json == null ? Optional.empty() : Optional.of(jsonMapper.readValue(json, Session.class));
	}

	@Override
	public void delete(String userId) {
		redis.delete(key(userId));
	}

	private static String key(String userId) {
		return KEY_PREFIX + userId;
	}
}
