package com.app.demo.auth.domain;

import java.time.Instant;

/**
 * 발급한 Access-Token.
 *
 * @param value     Authorization: Bearer 로 쓰는 JWT 문자열
 * @param jti       토큰마다 새로 만드는 고유 ID. 로그인 세션(Redis)에 저장해 "사용자당 최신 토큰 1개만 유효"를 판단한다
 * @param expiresAt 토큰 만료 시각 (세션 TTL 의 절대 상한)
 */
public record AccessToken(String value, String jti, Instant expiresAt) {
}
