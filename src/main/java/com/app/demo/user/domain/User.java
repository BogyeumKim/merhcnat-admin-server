package com.app.demo.user.domain;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * 사용자 애그리게이트(회사 운영 user 테이블 기준, 읽기 모델 + 판단 로직).
 * 상태 변경(실패 횟수 증가 등)은 원자성을 위해 {@link UserRepository} 의 의도 명시 메서드로 수행한다.
 *
 * @param password          평문 비교(로컬 전용, 운영 전 해시 전환 필요)
 * @param loginTypeCode     0: TOTP, 1: 카카오/SMS
 * @param passwordFailCount id/pw·OTP 오류 누적 횟수
 */
public record User(
		String userId,
		@Nullable String password,
		@Nullable Integer loginTypeCode,
		@Nullable Integer userType,
		int passwordFailCount,
		@Nullable String phoneNumber,
		@Nullable String totpSecret) {

	/** 누적 오류가 한도 이상이면 잠금 (올바른 비밀번호/OTP 여도 거부) */
	public boolean isLocked(int maxFailCount) {
		return passwordFailCount >= maxFailCount;
	}

	public boolean passwordMatches(String raw) {
		return password != null && password.equals(raw);
	}

	/** 지원하지 않는 유형이면 empty */
	public Optional<LoginType> loginType() {
		return LoginType.fromCode(loginTypeCode);
	}
}
