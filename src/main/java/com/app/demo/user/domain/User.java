package com.app.demo.user.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * 사용자 애그리게이트(회사 운영 user 테이블 기준, 읽기 모델 + 판단 로직).
 * 상태 변경(실패 횟수 증가 등)은 원자성을 위해 {@link UserRepository} 의 의도 명시 메서드로 수행한다.
 */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

	private final String userId;

	/** {@link PasswordHash} 로 만든 해시 */
	private final @Nullable String password;

	/** 0: TOTP, 1: 이메일, 2: SMS (1·2 는 {@link LoginType#MESSAGE}) */
	private final @Nullable Integer loginTypeCode;

	private final @Nullable Integer userType;

	/** id/pw·OTP 오류 누적 횟수 */
	private final int passwordFailCount;

	/** 카카오/SMS 인증번호 수신 번호 */
	private final @Nullable String phoneNumber;

	/** 이메일 인증번호 수신 주소 */
	private final @Nullable String email;

	private final @Nullable String totpSecret;

	/** 마지막 비밀번호 변경 시각 (기록이 없으면 null) */
	private final @Nullable LocalDateTime lastChangePw;

	/** 이전 비밀번호 최대 3개 (최신순, 비어 있는 칸은 제외) */
	private final List<String> oldPasswords;

	/** 이용 상태 (사용/정지/삭제). 코드표에 없거나 NULL 이면 null */
	private final @Nullable UseType useType;

	/** true 면 발급받은 임시비밀번호 (변경 전에는 로그인 완료 불가) */
	private final boolean tempPassword;

	public boolean isSuspended() {
		return useType == UseType.SUSPENDED;
	}

	public boolean isDeleted() {
		return useType == UseType.DELETED;
	}

	/** 사용 상태인지. 코드표에 없거나 NULL 이면 사용이 아니다 */
	public boolean isActive() {
		return useType == UseType.ACTIVE;
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

	/** 입력한 원문의 해시가 저장된 비밀번호와 같은지 */
	public boolean passwordMatches(String raw) {
		String hashed = PasswordHash.of(raw);
		return hashed.equals(password);
	}

	/** 재사용 금지: 현재 비밀번호 또는 이전 비밀번호(최근 3개)와 같으면 true */
	public boolean passwordReused(String raw) {
		String hashed = PasswordHash.of(raw);
		return hashed.equals(password) || oldPasswords.contains(hashed);
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
