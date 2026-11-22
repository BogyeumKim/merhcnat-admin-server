package com.app.demo.home.infrastructure;

import com.app.demo.home.domain.HomeSummary;
import com.app.demo.home.domain.HomeSummaryProvider;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** [더미] 거래/정산 데이터가 아직 없어 고정 샘플 값을 돌려준다. 입금 예정일만 내일로 계산. */
@Component
@RequiredArgsConstructor
class DummyHomeSummaryProvider implements HomeSummaryProvider {

	private final Clock clock;

	@Override
	public HomeSummary summaryOf(String userId) {
		return new HomeSummary("테스트 가맹점", 1_250_000L, 38, 2, 3_480_000L, LocalDate.now(clock).plusDays(1), 28_430_000L, true);
	}
}
