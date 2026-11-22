package com.app.demo.global.api;

import com.app.demo.global.exception.ErrorCode;
import org.jspecify.annotations.Nullable;

/**
 * 모든 API 공통 응답 봉투.
 * 요청(각 단계)이 성공하면 HTTP 200, 실패는 4xx/5xx 로 같은 봉투가 내려간다.
 *
 * @param code    앱이 분기하는 기계용 코드 (성공은 SUCCESS)
 * @param message 사람이 읽는 참고 문구
 * @param data    응답/오류 부가 정보 (없으면 응답에서 제외)
 */
public record ApiResponse<T>(String code, String message, @Nullable T data) {

	private static final String SUCCESS = "SUCCESS";

	/** 성공 응답 (data 없음) */
	public static <T> ApiResponse<T> ok() {
		return new ApiResponse<>(SUCCESS, "OK", null);
	}

	/** 성공 응답 (data 포함) */
	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(SUCCESS, "OK", data);
	}

	/** 성공 응답 (SUCCESS 외의 단계 code. 예: MFA_REQUIRED) */
	public static <T> ApiResponse<T> of(String code, String message, T data) {
		return new ApiResponse<>(code, message, data);
	}

	/** 오류 응답 (data 없음) */
	public static ApiResponse<ErrorDetail> error(ErrorCode errorCode) {
		return error(errorCode, null);
	}

	/** 오류 응답. 오류 봉투는 반드시 이 메서드로 만든다. data 는 {@link ErrorDetail} 구현 타입만 받는다. */
	public static ApiResponse<ErrorDetail> error(ErrorCode errorCode, @Nullable ErrorDetail data) {
		return error(errorCode, errorCode.getMessage(), data);
	}

	/** 오류 응답(상황별 문구를 쓸 때) */
	public static ApiResponse<ErrorDetail> error(ErrorCode errorCode, String message, @Nullable ErrorDetail data) {
		return new ApiResponse<>(errorCode.getCode(), message, data);
	}
}
