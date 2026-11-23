package com.app.demo.notice.domain;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/** 공지. body 는 일반 텍스트이며(HTML/마크다운 아님) 사진 한 장짜리 공지는 빈 문자열이다. */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Notice {

	/** 공지가 바뀌면 id 도 바뀌어야 앱의 "오늘 하루 보지 않기"가 새 공지에 적용되지 않는다 */
	private final String id;

	private final String title;

	private final String body;

	private final @Nullable NoticeImage image;

	/** 등록일 (홈의 최근 공지 표시용) */
	private final LocalDate createdOn;
}
