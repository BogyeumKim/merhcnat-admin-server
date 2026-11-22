package com.app.demo.global.exception;

import java.io.IOException;

/**
 * 요청 본문이 크기 상한을 넘었다(길이를 미리 알 수 없는 chunked 요청을 읽는 도중 감지).
 * 본문을 읽는 쪽(JSON 변환기)에 I/O 오류로 전달되며, GlobalExceptionHandler 가 원인을 보고 413 으로 바꾼다.
 */
public class PayloadTooLargeException extends IOException {

	public PayloadTooLargeException(long maxBytes) {
		super("Request body exceeds " + maxBytes + " bytes");
	}
}
