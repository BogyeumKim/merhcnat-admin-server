package com.app.demo.notice.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/** 공지 이미지. 앱이 로딩 전에 높이를 잡을 수 있도록 원본 픽셀 크기(width/height)가 필수다. */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NoticeImage {

	/** https 만 허용되는 CDN URL */
	private final String url;

	private final int width;

	private final int height;

	/** 스크린리더용 대체 텍스트 (장식용이면 null) */
	private final @Nullable String alt;
}
