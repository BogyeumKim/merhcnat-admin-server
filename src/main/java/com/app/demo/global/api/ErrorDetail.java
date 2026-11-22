package com.app.demo.global.api;

/**
 * 오류 응답 봉투의 data 타입. 오류에 함께 내려줄 부가 정보는 모두 이 타입을 구현한다
 * ({@link ErrorData}, 점검 시각, 필수 버전 등). 오류 봉투는 {@code ApiResponse<ErrorDetail>} 로 타입이 고정된다.
 *
 * <p>구현 타입이 여러 패키지(global, appstatus)에 있어 sealed 로 닫지 않는다(이름 없는 모듈에서 sealed 의 허용 타입은 같은 패키지여야 한다).
 */
public interface ErrorDetail {
}
