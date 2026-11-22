package com.app.demo.auth.domain;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * 사용자당 하나뿐인 로그인 세션. 새로 로그인하면 통째로 교체되어 이전 기기의 토큰은 즉시 무효가 된다.
 *
 * @param jti          현재 유효한 Access-Token 의 jti
 * @param deviceIdHash 로그인한 기기 ID 의 SHA-256 hex (원문은 저장하지 않는다)
 * @param issuedAt     로그인 완료 시각
 */
public record Session(String jti, @Nullable String deviceIdHash, Instant issuedAt) {

	/** 요청에 쓰인 토큰이 이 세션의 토큰인지 */
	public boolean isCurrent(String tokenJti) {
		return jti.equals(tokenJti);
	}
}
