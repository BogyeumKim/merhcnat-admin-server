package com.app.demo.global.exception;

import com.app.demo.global.api.ApiResponse;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.coyote.ActionCode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tomcat 이 Spring 에 넘기기 전에 거부한 요청(인코딩된 '/', 잘못된 헤더 문자, 너무 큰 헤더, 잘못된 요청 줄 등)의 오류 응답.
 * 기본 ErrorReportValve 는 서버 버전이 들어간 HTML 을 내려주므로, 같은 상황을 공통 JSON 봉투로 바꾼다.
 * 응답 본문에는 서버/프레임워크 정보·예외 내용을 넣지 않는다. 구현은 Tomcat 의 JsonErrorReportValve 와 같은 절차를 따른다.
 */
public class EnvelopeErrorReportValve extends ErrorReportValve {

	private final JsonMapper jsonMapper;

	public EnvelopeErrorReportValve(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
		setShowReport(false);
		setShowServerInfo(false);
	}

	@Override
	protected void report(Request request, Response response, Throwable throwable) {
		int status = response.getStatus();
		if (status < 400 || response.getContentWritten() > 0 || !response.setErrorReported()) {
			return; // 성공 응답이거나 이미 본문을 쓴 응답(Spring 이 처리한 오류)은 건드리지 않는다
		}
		AtomicBoolean ioAllowed = new AtomicBoolean(false);
		response.getCoyoteResponse().action(ActionCode.IS_IO_ALLOWED, ioAllowed);
		if (!ioAllowed.get()) {
			return;
		}
		CommonErrorCode errorCode = CommonErrorCode.fromStatus(status);
		try {
			response.setContentType("application/json");
			response.setCharacterEncoding(StandardCharsets.UTF_8);
			Writer writer = response.getReporter();
			if (writer != null) {
				writer.write(jsonMapper.writeValueAsString(ApiResponse.error(errorCode)));
				response.finishResponse();
			}
		} catch (IOException | IllegalStateException e) {
			// 연결이 이미 끊긴 경우 등: 더 할 수 있는 것이 없다(기본 밸브와 동일하게 무시)
		}
	}
}
