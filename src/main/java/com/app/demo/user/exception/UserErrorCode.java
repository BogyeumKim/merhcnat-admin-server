package com.app.demo.user.exception;

import com.app.demo.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 기기·푸시 토큰 오류. 모두 400(화면 오류)이고 구체적인 화면은 code 로 분기한다. */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

	DEVICE_REQUIRED(HttpStatus.BAD_REQUEST, "DEVICE_REQUIRED", "기기 정보가 필요합니다."),
	DEVICE_NOT_IN_TOKEN(HttpStatus.BAD_REQUEST, "DEVICE_REQUIRED", "기기 정보가 없는 토큰입니다."),
	DEVICE_INVALID(HttpStatus.BAD_REQUEST, "DEVICE_INVALID", "지원하지 않는 기기입니다."),
	DEVICE_NOT_FOUND(HttpStatus.BAD_REQUEST, "DEVICE_NOT_FOUND", "등록된 기기가 아닙니다."),
	DEVICE_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "DEVICE_NOT_VERIFIED", "등록된 기기임을 확인할 수 없습니다."),
	PUSH_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "PUSH_TOKEN_INVALID", "올바르지 않은 푸시 토큰입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
