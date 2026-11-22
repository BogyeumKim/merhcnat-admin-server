package com.app.demo.home.domain;

/** 홈 요약 값 조회 포트. 실제 거래/정산 데이터 연동이 생기면 구현체만 교체한다. */
public interface HomeSummaryProvider {

	HomeSummary summaryOf(String userId);
}
