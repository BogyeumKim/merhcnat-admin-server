package com.app.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 진입점. DDD 패키지 구조:
 * <pre>
 * global/            공통(응답 봉투, 예외 처리, 설정)
 * user/              사용자 컨텍스트 (domain / infrastructure)
 * auth/              인증 컨텍스트  (domain / application / infrastructure / presentation)
 * device/            기기·푸시 컨텍스트 (application / presentation)
 * </pre>
 * 의존 방향: presentation → application → domain ← infrastructure
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
