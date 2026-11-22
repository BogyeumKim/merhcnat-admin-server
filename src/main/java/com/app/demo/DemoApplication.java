package com.app.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 진입점. DDD 패키지 구조 (맥락별로 같은 4개 층을 둔다):
 * <pre>
 * global/            공통: api(응답 봉투) / exception / security(AuthUser) / config
 * auth/              인증: 로그인·OTP(MFA)·비밀번호
 * user/              사용자: 사용자·부가정보·등록 기기(푸시 토큰)
 * notice/            공지
 * home/              홈 요약
 *
 * 각 맥락 내부:
 *   api/             컨트롤러
 *   api/dto/         요청(Request)·응답(Response) DTO
 *   application/     유스케이스 서비스, Command/Result
 *   domain/          도메인 모델, 저장소·외부연동 인터페이스
 *   infrastructure/  인터페이스 구현 (persistence: MyBatis 엔티티·매퍼)
 *   config/          설정 프로퍼티·보안 설정 (필요한 맥락만)
 * </pre>
 * 의존 방향: api → application → domain ← infrastructure
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
