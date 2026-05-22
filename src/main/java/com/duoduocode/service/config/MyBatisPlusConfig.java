package com.duoduocode.service.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置类
 */
@Configuration
@MapperScan("com.duoduocode.service.**.mapper")
public class MyBatisPlusConfig {
}
