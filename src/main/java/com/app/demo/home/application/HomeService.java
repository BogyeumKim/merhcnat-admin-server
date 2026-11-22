package com.app.demo.home.application;

import com.app.demo.home.domain.HomeSummary;
import com.app.demo.home.domain.HomeSummaryProvider;
import com.app.demo.notice.application.NoticeService;
import com.app.demo.notice.domain.Notice;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 홈 화면 요약 유스케이스: 요약 값 + 최근 공지 1건 */
@Service
@RequiredArgsConstructor
public class HomeService {

	private final HomeSummaryProvider summaries;
	private final NoticeService noticeService;

	public HomeSummary summary(String userId) {
		return summaries.summaryOf(userId);
	}

	public Optional<Notice> latestNotice() {
		return noticeService.latestNotice();
	}
}
