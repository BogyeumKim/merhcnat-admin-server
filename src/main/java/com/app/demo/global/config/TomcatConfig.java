package com.app.demo.global.config;

import com.app.demo.global.exception.EnvelopeErrorReportValve;
import org.apache.catalina.Valve;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.valves.ErrorReportValve;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import tools.jackson.databind.json.JsonMapper;

/**
 * 내장 Tomcat 설정: 기본 오류 밸브(HTML + 서버 정보)를 JSON 오류 응답(ErrorResponse) 밸브로 교체한다.
 * Spring Boot 가 추가한 ErrorReportValve 까지 지우도록 가장 마지막 순서로 적용한다.
 */
@Configuration
public class TomcatConfig {

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	WebServerFactoryCustomizer<TomcatServletWebServerFactory> envelopeErrorReportValve(JsonMapper jsonMapper) {
		return factory -> factory.addContextCustomizers(context -> {
			if (context.getParent() instanceof StandardHost host) {
				for (Valve valve : host.getPipeline().getValves()) {
					if (valve instanceof ErrorReportValve) {
						host.getPipeline().removeValve(valve);
					}
				}
				// Host 가 기동할 때 기본 밸브를 다시 넣지 않도록 클래스 이름도 지정한다
				host.setErrorReportValveClass(EnvelopeErrorReportValve.class.getName());
				host.getPipeline().addValve(new EnvelopeErrorReportValve(jsonMapper));
			}
		});
	}
}
