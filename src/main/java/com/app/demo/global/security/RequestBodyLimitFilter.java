package com.app.demo.global.security;

import com.app.demo.global.api.ErrorResponseWriter;
import com.app.demo.global.exception.CommonErrorCode;
import com.app.demo.global.exception.LogSafe;
import com.app.demo.global.exception.PayloadTooLargeException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청 본문 크기 상한(64KB). Tomcat 의 maxPostSize 는 폼 전송에만 적용되고 JSON 본문에는 상한이 없어서 직접 막는다.
 * <ul>
 *   <li>Content-Length 가 상한을 넘으면 본문을 읽기 전에 413 PAYLOAD_TOO_LARGE 로 응답한다.</li>
 *   <li>길이를 알 수 없는 chunked 요청은 읽는 도중 상한을 넘는 순간 {@link PayloadTooLargeException} 을 던지고,
 *       GlobalExceptionHandler 가 413 으로 바꾼다.</li>
 * </ul>
 * 모든 필터(Logbook 본문 로그·감사·보안)보다 먼저 실행해 큰 본문이 버퍼링·인증 처리까지 가지 않게 한다.
 * (Logbook 의 secureLogbookFilter 는 {@code HIGHEST_PRECEDENCE + 1}, logbookFilter 는 {@code LOWEST_PRECEDENCE})
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RequestBodyLimitFilter extends OncePerRequestFilter {

	static final long MAX_BODY_BYTES = 64 * 1024;

	private final ErrorResponseWriter writer;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		long contentLength = request.getContentLengthLong();
		if (contentLength > MAX_BODY_BYTES) {
			log.warn("[{}] {} - Content-Length {}", CommonErrorCode.PAYLOAD_TOO_LARGE.getCode(), LogSafe.target(request), contentLength);
			writer.write(response, CommonErrorCode.PAYLOAD_TOO_LARGE);
			return;
		}
		if (contentLength >= 0) {
			chain.doFilter(request, response);
			return;
		}
		// 길이를 모르는(chunked) 본문: 상한을 넘으면 읽는 쪽에서 예외가 난다. Logbook 처럼 MVC 앞의 필터가 본문을 먼저 읽으면
		// GlobalExceptionHandler 를 거치지 않으므로 여기서 413 으로 바꾼다.
		try {
			chain.doFilter(new LimitedBodyRequest(request), response);
		} catch (PayloadTooLargeException _) {
			log.warn("[{}] {} - chunked body over limit", CommonErrorCode.PAYLOAD_TOO_LARGE.getCode(), LogSafe.target(request));
			if (!response.isCommitted()) {
				response.reset();
				writer.write(response, CommonErrorCode.PAYLOAD_TOO_LARGE);
			}
		}
	}

	/** 길이를 모르는 본문을 읽는 동안 상한을 넘으면 중단시키는 요청 래퍼 */
	private static final class LimitedBodyRequest extends HttpServletRequestWrapper {

		private LimitedBodyRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			return new LimitedInputStream(super.getInputStream());
		}
	}

	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	private static final class LimitedInputStream extends ServletInputStream {

		private final ServletInputStream delegate;
		private long read;

		@Override
		public int read() throws IOException {
			int b = delegate.read();
			if (b >= 0) {
				count(1);
			}
			return b;
		}

		@Override
		public int read(byte[] buffer, int offset, int length) throws IOException {
			int n = delegate.read(buffer, offset, length);
			if (n > 0) {
				count(n);
			}
			return n;
		}

		private void count(int n) throws PayloadTooLargeException {
			read += n;
			if (read > MAX_BODY_BYTES) {
				throw new PayloadTooLargeException(MAX_BODY_BYTES);
			}
		}

		@Override
		public boolean isFinished() {
			return delegate.isFinished();
		}

		@Override
		public boolean isReady() {
			return delegate.isReady();
		}

		@Override
		public void setReadListener(ReadListener listener) {
			delegate.setReadListener(listener);
		}
	}
}
