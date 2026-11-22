package com.app.demo.appstatus.exception;

import com.app.demo.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 점검(503)·버전 차단(403) 오류. 로그인 전/후 모든 API(/api/**)에 적용된다. 403 은 UPDATE_REQUIRED 전용이다. */
@Getter
@RequiredArgsConstructor
public enum AppStatusErrorCode implements ErrorCode {

	MAINTENANCE(HttpStatus.SERVICE_UNAVAILABLE, "MAINTENANCE", "서비스 점검 중입니다. 잠시 후 다시 이용해 주세요."),
	UPDATE_REQUIRED(HttpStatus.FORBIDDEN, "UPDATE_REQUIRED", "새 버전으로 업데이트해 주세요.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
