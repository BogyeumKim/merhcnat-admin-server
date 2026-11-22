package com.app.demo.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 스케줄 작업({@code @Scheduled}) 활성화. 예: 앱 점검·버전 정책 스냅샷 주기 갱신 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
