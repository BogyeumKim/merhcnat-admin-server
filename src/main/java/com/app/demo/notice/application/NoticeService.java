package com.app.demo.notice.application;

import com.app.demo.notice.domain.Notice;
import com.app.demo.notice.domain.NoticeRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 공지 조회 유스케이스. 노출 대상/기간/우선순위 선별은 서버가 하고 앱은 거르지 않는다. */
@Service
@RequiredArgsConstructor
public class NoticeService {

	private final NoticeRepository notices;
	private final Clock clock;

	/** 로그인 후 보여줄 팝업 공지 (없으면 빈 목록) */
	public List<Notice> popups() {
		return notices.findPopups(LocalDateTime.now(clock));
	}

	/** 노출 중인 가장 최근 공지 1건 (홈 요약용) */
	public Optional<Notice> latestNotice() {
		return notices.findLatest(LocalDateTime.now(clock));
	}
}
