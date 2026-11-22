package com.app.demo.global.exception;

import com.app.demo.global.api.ErrorResponseWriter;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서블릿 오류 페이지(/error)를 공통 봉투로 응답한다. Spring Boot 기본 {@code BasicErrorController}(timestamp/path 형식)를 대신한다.
 * 컨트롤러 밖에서 {@code sendError} 로 끝난 요청이 여기로 온다. 예: Tomcat 이 거부한 TRACE(405), 필터 단계 오류.
 * 클라이언트의 Accept 와 상관없이 항상 JSON 으로 쓰기 위해 메시지 변환기를 거치지 않고 응답에 직접 쓴다.
 * 상태 코드 없이 /error 를 직접 부르면(인증된 사용자만 가능) 없는 경로와 같은 404 로 응답한다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class EnvelopeErrorController implements ErrorController {

	private final ErrorResponseWriter writer;

	@RequestMapping("${server.error.path:/error}")
	public void error(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		CommonErrorCode errorCode = CommonErrorCode.fromStatus(status instanceof Integer code ? code : 404);
		Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
		log.warn("[{}] {} {} - error dispatch", errorCode.getCode(), LogSafe.clean(request.getMethod()),
				LogSafe.clean(uri instanceof String s ? s : request.getRequestURI()));
		writer.write(response, errorCode);
	}
}
