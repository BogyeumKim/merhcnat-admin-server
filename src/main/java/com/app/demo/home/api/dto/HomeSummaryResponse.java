package com.app.demo.home.api.dto;

import com.app.demo.home.domain.HomeSummary;
import com.app.demo.notice.domain.Notice;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/** GET /api/home/summary 응답 data. 날짜는 ISO(yyyy-MM-dd), 금액은 원 단위 정수. 최근 공지가 없으면 latestNotice 는 응답에서 제외. */
@JsonPropertyOrder({"storeName", "todaySales", "approvalCount", "cancellationCount", "nextDepositAmount", "nextDepositDate", "monthSales", "prepayAvailable", "latestNotice"})
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HomeSummaryResponse {

	private final String storeName;

	private final long todaySales;

	private final int approvalCount;

	private final int cancellationCount;

	private final long nextDepositAmount;

	private final String nextDepositDate;

	private final long monthSales;

	private final boolean prepayAvailable;

	private final @Nullable LatestNotice latestNotice;

	public static HomeSummaryResponse of(HomeSummary s, @Nullable Notice notice) {
		String nextDepositDate = s.getNextDepositDate().toString();
		LatestNotice latestNotice = notice == null ? null : LatestNotice.from(notice);
		return HomeSummaryResponse.with()
				.storeName(s.getStoreName())
				.todaySales(s.getTodaySales())
				.approvalCount(s.getApprovalCount())
				.cancellationCount(s.getCancellationCount())
				.nextDepositAmount(s.getNextDepositAmount())
				.nextDepositDate(nextDepositDate)
				.monthSales(s.getMonthSales())
				.prepayAvailable(s.isPrepayAvailable())
				.latestNotice(latestNotice)
				.create();
	}

	@JsonPropertyOrder({"id", "title", "date"})
	@Getter
	@Builder(builderMethodName = "with", buildMethodName = "create")
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class LatestNotice {

		private final String id;

		private final String title;

		private final String date;

		static LatestNotice from(Notice notice) {
			String date = notice.getCreatedOn().toString();
			return LatestNotice.with()
					.id(notice.getId())
					.title(notice.getTitle())
					.date(date)
					.create();
		}
	}
}
