package com.app.demo.notice.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 공지 저장소 포트 */
public interface NoticeRepository {

	/** 노출 기간 안에 있는 팝업 공지를 우선순위(작은 값이 먼저) → 최신순으로 돌려준다. */
	List<Notice> findPopups(LocalDateTime now);

	/** 노출 중인 가장 최근 공지 1건 (홈 요약용) */
	Optional<Notice> findLatest(LocalDateTime now);
}
