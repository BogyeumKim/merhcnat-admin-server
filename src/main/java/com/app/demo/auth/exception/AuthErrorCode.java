package com.app.demo.auth.exception;

import com.app.demo.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 로그인·OTP·비밀번호 오류. status 는 앱의 공통 행동(400 화면 오류 / 401 인증·세션)만 나누고, 구체적인 화면은 code 로 분기한다. */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

	// 로그인 / 계정 상태
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다."),
	ACCOUNT_LOCKED(HttpStatus.BAD_REQUEST, "ACCOUNT_LOCKED", "인증 오류가 누적되어 계정이 잠겼습니다. 관리자에게 문의해 주세요."),
	ACCOUNT_SUSPENDED(HttpStatus.BAD_REQUEST, "ACCOUNT_SUSPENDED", "정지된 사용자입니다. 고객센터로 문의해 주세요."),
	LOGIN_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "LOGIN_NOT_ALLOWED", "로그인 권한이 없습니다."),
	UNSUPPORTED_LOGIN_TYPE(HttpStatus.BAD_REQUEST, "UNSUPPORTED_LOGIN_TYPE", "지원하지 않는 인증 유형입니다."),

	// 로그인 세션 (Redis, 사용자당 최신 Access-Token 1개만 유효)
	SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED", "세션이 만료되었습니다. 다시 로그인해 주세요."),
	SESSION_REPLACED(HttpStatus.UNAUTHORIZED, "SESSION_REPLACED", "다른 기기에서 로그인되어 로그아웃되었습니다."),

	// 챌린지
	CHALLENGE_EXPIRED(HttpStatus.UNAUTHORIZED, "CHALLENGE_EXPIRED", "인증 시간이 만료되었습니다. 처음부터 다시 로그인해 주세요."),

	// OTP
	OTP_INVALID(HttpStatus.UNAUTHORIZED, "OTP_INVALID", "인증번호가 올바르지 않습니다."),
	OTP_EXPIRED(HttpStatus.UNAUTHORIZED, "OTP_EXPIRED", "인증번호가 만료되었습니다. 다시 요청해 주세요."),
	OTP_CHANNEL_INVALID(HttpStatus.BAD_REQUEST, "OTP_CHANNEL_INVALID", "허용되지 않는 인증 수단입니다."),
	OTP_CHANNEL_NOT_SENDABLE(HttpStatus.BAD_REQUEST, "OTP_CHANNEL_INVALID", "TOTP는 발송이 필요 없습니다."),
	OTP_SEND_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "OTP_SEND_LIMIT_EXCEEDED", "발송 횟수를 초과했습니다. 처음부터 다시 로그인해 주세요."),
	TOTP_NOT_ENROLLED(HttpStatus.BAD_REQUEST, "TOTP_NOT_ENROLLED", "TOTP가 등록되지 않았습니다."),

	// 비밀번호
	CURRENT_PASSWORD_INVALID(HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_INVALID", "현재 비밀번호가 올바르지 않습니다."),
	PASSWORD_POLICY_VIOLATION(HttpStatus.BAD_REQUEST, "PASSWORD_POLICY_VIOLATION", "비밀번호 규칙에 맞지 않습니다."),
	PASSWORD_REUSED(HttpStatus.BAD_REQUEST, "PASSWORD_REUSED", "최근에 사용한 비밀번호는 사용할 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
