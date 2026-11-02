package com.app.demo.global.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/** MyBatis-Plus 매퍼 스캔 설정. 매퍼는 각 컨텍스트의 infrastructure.persistence 에 둔다. */
@Configuration
@MapperScan("com.app.demo.user.infrastructure.persistence")
public class PersistenceConfig {
}
