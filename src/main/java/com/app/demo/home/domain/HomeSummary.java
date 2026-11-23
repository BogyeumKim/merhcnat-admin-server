package com.app.demo.home.domain;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 홈 화면 요약 값. 금액은 원 단위 정수. */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HomeSummary {

	/** 가맹점명 */
	private final String storeName;

	/** 오늘 매출 */
	private final long todaySales;

	/** 오늘 승인 건수 */
	private final int approvalCount;

	/** 오늘 취소 건수 */
	private final int cancellationCount;

	/** 다음 입금 예정 금액 */
	private final long nextDepositAmount;

	/** 다음 입금 예정일 */
	private final LocalDate nextDepositDate;

	/** 이번 달 매출 */
	private final long monthSales;

	/** 선지급 프로모션 카드 노출 여부 */
	private final boolean prepayAvailable;
}
