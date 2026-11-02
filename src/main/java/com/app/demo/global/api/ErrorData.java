package com.app.demo.global.api;

import org.jspecify.annotations.Nullable;

/**
 * 오류 응답 data 의 확장 필드. 해당하는 값만 채운다.
 *
 * @param remainingAttempts 잠금까지 남은 시도 횟수
 * @param retryAfterSeconds 다시 시도 가능할 때까지 대기 초
 */
public record ErrorData(@Nullable Integer remainingAttempts, @Nullable Long retryAfterSeconds) {

	public static ErrorData remaining(int remainingAttempts) {
		return new ErrorData(remainingAttempts, null);
	}

	public static ErrorData retryAfter(long seconds) {
		return new ErrorData(null, seconds);
	}
}
