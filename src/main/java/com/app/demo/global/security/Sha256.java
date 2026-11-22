package com.app.demo.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.jspecify.annotations.Nullable;

/** 식별자·비밀 값을 원문 대신 저장/전달할 때 쓰는 SHA-256 해시와 상수 시간 비교. */
public final class Sha256 {

	private Sha256() {
	}

	/** SHA-256 hex(소문자 64자) */
	public static String hex(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 을 사용할 수 없습니다", e);
		}
	}

	/** 원문의 해시가 저장된 해시와 같은지 상수 시간으로 비교한다(타이밍 공격 방지). 어느 쪽이든 없으면 false */
	public static boolean matches(@Nullable String raw, @Nullable String storedHex) {
		if (raw == null || storedHex == null) {
			return false;
		}
		return MessageDigest.isEqual(hex(raw).getBytes(StandardCharsets.UTF_8), storedHex.getBytes(StandardCharsets.UTF_8));
	}
}
