package com.app.demo.global.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Spring 에 도달하기 전 Tomcat 단계에서 거부되는 요청과, 실제 서블릿 컨테이너에서만 재현되는 경우(TRACE, chunked 본문)를
 * 원시 소켓으로 보내 검증한다. 어떤 경우에도 HTML·서버 버전 없이 JSON 오류 응답(ErrorResponse)이 나가야 한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TomcatLevelErrorTest {

	@LocalServerPort
	int port;

	@Test
	void encodedSlashIs400Envelope() throws IOException {
		assertEnvelope(send("GET /api/a%2Fb HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n"), 400, "BAD_REQUEST");
	}

	@Test
	void controlCharacterInHeaderIs400Envelope() throws IOException {
		assertEnvelope(send("GET /api/home/summary HTTP/1.1\r\nHost: x\r\nX-Bad: a\u0001b\r\nConnection: close\r\n\r\n"),
				400, "BAD_REQUEST");
	}

	@Test
	void oversizedHeaderIs400Envelope() throws IOException {
		assertEnvelope(send("GET /api/home/summary HTTP/1.1\r\nHost: x\r\nX-Big: " + "a".repeat(20_000)
				+ "\r\nConnection: close\r\n\r\n"), 400, "BAD_REQUEST");
	}

	@Test
	void malformedRequestLineIs400Envelope() throws IOException {
		assertEnvelope(send("GET /api/home summary HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n"), 400, "BAD_REQUEST");
	}

	@Test
	void traceIsRejectedWith405Envelope() throws IOException {
		assertEnvelope(send("TRACE /api/home/summary HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n"), 405,
				"METHOD_NOT_ALLOWED");
	}

	@Test
	void firewallRejectionIs400Envelope() throws IOException {
		assertEnvelope(send("GET /api//home/summary HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n"), 400, "BAD_REQUEST");
		assertEnvelope(send("GET /api/home;x=1/summary HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n"), 400, "BAD_REQUEST");
	}

	@Test
	void unauthenticatedProbeIs401Envelope() throws IOException {
		assertEnvelope(send("GET /actuator/env HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n"), 401, "UNAUTHORIZED");
	}

	private static void assertEnvelope(String raw, int status, String code) {
		String head = raw.substring(0, Math.max(raw.indexOf("\r\n\r\n"), 0)).toLowerCase(Locale.ROOT);
		assertThat(raw).startsWith("HTTP/1.1 " + status + " ");
		assertThat(head).contains("content-type: application/json");
		assertThat(head).doesNotContain("\r\nserver:");
		assertThat(raw).contains("\"code\":\"" + code + "\"").doesNotContainIgnoringCase("tomcat").doesNotContain("<html");
	}

	private String send(String request) throws IOException {
		try (Socket socket = new Socket("localhost", port)) {
			socket.setSoTimeout(10_000);
			OutputStream out = socket.getOutputStream();
			out.write(request.getBytes(StandardCharsets.ISO_8859_1));
			out.flush();
			InputStream in = socket.getInputStream();
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			in.transferTo(buffer);
			return buffer.toString(StandardCharsets.UTF_8);
		}
	}
}
