package com.app.demo.home.api.dto;

import com.app.demo.home.domain.HomeSummary;
import com.app.demo.notice.domain.Notice;
import org.jspecify.annotations.Nullable;

/** GET /api/home/summary 응답 data. 날짜는 ISO(yyyy-MM-dd), 금액은 원 단위 정수. 최근 공지가 없으면 latestNotice 는 응답에서 제외. */
public record HomeSummaryResponse(
		String storeName,
		long todaySales,
		int approvalCount,
		int cancellationCount,
		long nextDepositAmount,
		String nextDepositDate,
		long monthSales,
		boolean prepayAvailable,
		@Nullable LatestNotice latestNotice) {

	public static HomeSummaryResponse of(HomeSummary s, @Nullable Notice notice) {
		return new HomeSummaryResponse(s.storeName(), s.todaySales(), s.approvalCount(), s.cancellationCount(),
				s.nextDepositAmount(), s.nextDepositDate().toString(), s.monthSales(), s.prepayAvailable(),
				notice == null ? null : new LatestNotice(notice.id(), notice.title(), notice.createdOn().toString()));
	}

	public record LatestNotice(String id, String title, String date) {
	}
}
