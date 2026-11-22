package com.app.demo.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 앱에 내려가는 오류 정의. 맥락별 enum(CommonErrorCode, AuthErrorCode, UserErrorCode …)이 구현한다.
 * code 는 앱이 분기하는 계약이므로 함부로 바꾸지 않는다. 같은 code 라도 상황별 문구가 다르면 enum 상수를 나눈다.
 */
public interface ErrorCode {

	HttpStatus getStatus();

	String getCode();

	String getMessage();
}
