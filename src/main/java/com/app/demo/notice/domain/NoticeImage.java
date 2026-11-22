package com.app.demo.notice.domain;

import org.jspecify.annotations.Nullable;

/**
 * 공지 이미지. 앱이 로딩 전에 높이를 잡을 수 있도록 원본 픽셀 크기(width/height)가 필수다.
 *
 * @param url https 만 허용되는 CDN URL
 * @param alt 스크린리더용 대체 텍스트 (장식용이면 null)
 */
public record NoticeImage(String url, int width, int height, @Nullable String alt) {
}
