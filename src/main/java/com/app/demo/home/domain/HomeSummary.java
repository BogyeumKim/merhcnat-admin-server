package com.app.demo.home.domain;

import java.time.LocalDate;

/**
 * 홈 화면 요약 값. 금액은 원 단위 정수.
 *
 * @param storeName         가맹점명
 * @param todaySales        오늘 매출
 * @param approvalCount     오늘 승인 건수
 * @param cancellationCount 오늘 취소 건수
 * @param nextDepositAmount 다음 입금 예정 금액
 * @param nextDepositDate   다음 입금 예정일
 * @param monthSales        이번 달 매출
 * @param prepayAvailable   선지급 프로모션 카드 노출 여부
 */
public record HomeSummary(
		String storeName,
		long todaySales,
		int approvalCount,
		int cancellationCount,
		long nextDepositAmount,
		LocalDate nextDepositDate,
		long monthSales,
		boolean prepayAvailable) {
}
