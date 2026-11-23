package com.app.demo.global.api;

import com.app.demo.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring MVC 바깥(필터, 보안 처리기, 오류 페이지, Tomcat 밸브)에서 오류 봉투를 응답에 직접 쓴다.
 * 컨트롤러 안의 오류는 GlobalExceptionHandler 가 ResponseEntity 로 만들고, 여기는 그 밖의 경로를 같은 형식으로 맞춘다.
 */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

	private final JsonMapper jsonMapper;

	public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		write(response, errorCode, errorCode.getMessage(), null);
	}

	/** 상황별 문구·부가 정보(data)를 담아 쓴다. data 가 null 이면 응답에서 제외된다. */
	public void write(HttpServletResponse response, ErrorCode errorCode, String message, @Nullable ErrorDetail data) throws IOException {
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		jsonMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode, message, data));
	}
}
