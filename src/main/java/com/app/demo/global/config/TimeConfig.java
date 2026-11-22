package com.app.demo.global.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 현재 시각의 단일 출처. 코드에서는 now() 대신 주입받은 Clock 을 써서 테스트에서 시간을 고정할 수 있게 한다.
 * 시간대는 DB 접속 설정(serverTimezone=Asia/Seoul)과 맞춘다.
 */
@Configuration
public class TimeConfig {

	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	@Bean
	public Clock clock() {
		return Clock.system(ZONE);
	}
}
