package com.app.demo.user.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * 사용자 애그리게이트(회사 운영 user 테이블 기준, 읽기 모델 + 판단 로직).
 * 상태 변경(실패 횟수 증가 등)은 원자성을 위해 {@link UserRepository} 의 의도 명시 메서드로 수행한다.
 *
 * @param password          평문 비교(로컬 전용, 운영 전 해시 전환 필요)
 * @param loginTypeCode     0: TOTP, 1: 카카오/SMS
 * @param passwordFailCount id/pw·OTP 오류 누적 횟수
 * @param phoneNumber       카카오/SMS 인증번호 수신 번호
 * @param email             이메일 인증번호 수신 주소
 * @param lastChangePw      마지막 비밀번호 변경 시각 (기록이 없으면 null)
 * @param oldPasswords      이전 비밀번호 최대 3개 (최신순, 비어 있는 칸은 제외)
 * @param useType           이용 상태 (사용/정지/삭제)
 * @param tempPassword      true 면 발급받은 임시비밀번호 (변경 전에는 로그인 완료 불가)
 */
public record User(
		String userId,
		@Nullable String password,
		@Nullable Integer loginTypeCode,
		@Nullable Integer userType,
		int passwordFailCount,
		@Nullable String phoneNumber,
		@Nullable String email,
		@Nullable String totpSecret,
		@Nullable LocalDateTime lastChangePw,
		List<String> oldPasswords,
		UseType useType,
		boolean tempPassword) {

	public boolean isSuspended() {
		return useType == UseType.SUSPENDED;
	}

	public boolean isDeleted() {
		return useType == UseType.DELETED;
	}

	/** 휴대폰 번호가 등록돼 있는지 (공백만 있으면 없는 것으로 본다) */
	public boolean hasPhone() {
		return phoneNumber != null && !phoneNumber.isBlank();
	}

	/** 이메일이 등록돼 있는지 (공백만 있으면 없는 것으로 본다) */
	public boolean hasEmail() {
		return email != null && !email.isBlank();
	}

	/** 누적 오류가 한도 이상이면 잠금 (올바른 비밀번호/OTP 여도 거부) */
	public boolean isLocked(int maxFailCount) {
		return passwordFailCount >= maxFailCount;
	}

	public boolean passwordMatches(String raw) {
		return password != null && password.equals(raw);
	}

	/** 재사용 금지: 현재 비밀번호 또는 이전 비밀번호(최근 3개)와 같으면 true */
	public boolean passwordReused(String raw) {
		return passwordMatches(raw) || oldPasswords.contains(raw);
	}

	/** 마지막 변경 후 expireDays 일이 지났는지 (변경 기록이 없으면 만료되지 않은 것으로 본다) */
	public boolean isPasswordExpired(LocalDateTime now, int expireDays) {
		return lastChangePw != null && lastChangePw.plusDays(expireDays).isBefore(now);
	}

	/**
	 * 새 비밀번호를 등록하기 전에는 로그인을 완료할 수 없는 이유. 임시비밀번호가 만료보다 우선한다.
	 *
	 * @return 변경이 필요 없으면 empty
	 */
	public Optional<PasswordChangeReason> passwordChangeReason(LocalDateTime now, int expireDays) {
		if (tempPassword) {
			return Optional.of(PasswordChangeReason.TEMP_PASSWORD);
		}
		if (isPasswordExpired(now, expireDays)) {
			return Optional.of(PasswordChangeReason.PASSWORD_EXPIRED);
		}
		return Optional.empty();
	}

	/** 지원하지 않는 유형이면 empty */
	public Optional<LoginType> loginType() {
		return LoginType.fromCode(loginTypeCode);
	}
}
