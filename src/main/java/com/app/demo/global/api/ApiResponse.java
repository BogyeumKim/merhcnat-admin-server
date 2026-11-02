package com.app.demo.global.api;

import org.jspecify.annotations.Nullable;

/**
 * 모든 API 공통 응답 봉투.
 * HTTP 200 은 "요청이 실제로 성공"했을 때만 쓰고, 그 외는 다른 HTTP status 와 같은 봉투로 내려간다.
 *
 * @param code    앱이 분기하는 기계용 코드 (성공은 SUCCESS)
 * @param message 사람이 읽는 참고 문구
 * @param data    응답/오류 부가 정보 (없으면 응답에서 제외)
 */
public record ApiResponse<T>(String code, String message, @Nullable T data) {

	/** 성공 응답 (data 없음) */
	public static <T> ApiResponse<T> ok() {
		return new ApiResponse<>("SUCCESS", "OK", null);
	}

	/** 성공 응답 (data 포함) */
	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>("SUCCESS", "OK", data);
	}
}
