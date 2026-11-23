package com.app.demo.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.app.demo.auth.application.LoginService;
import com.app.demo.auth.application.PasswordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Logbook 요청 로그는 비밀번호·기기 비밀키·토큰 값을 가리고 키 이름만 남긴다. */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class LogbookMaskingTest {

	private static final String LOGIN_BODY = """
			{"userId":"logbook_test","password":"Plain1234!","deviceSecret":"secret-value-xyz",
			 "device":{"deviceId":"logbook-device","platform":"IOS"}}
			""";

	@Autowired
	MockMvc mvc;

	@MockitoBean
	LoginService loginService;

	@MockitoBean
	PasswordService passwordService;

	@Test
	@DisplayName("요청 본문의 password·deviceSecret 은 로그에 원문 대신 **** 로 남는다")
	void masksSensitiveBodyFields(CapturedOutput output) throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY));

		String logs = output.getAll();
		assertThat(logs).contains("logbook_test");
		assertThat(logs).doesNotContain("Plain1234!");
		assertThat(logs).doesNotContain("secret-value-xyz");
		assertThat(logs).contains("****");
	}
}
