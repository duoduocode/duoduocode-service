package com.duoduocode.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Duoduo Service 主启动类
 *
 * @author duoduo
 */
@SpringBootApplication
@EnableTransactionManagement
public class DuoduoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DuoduoServiceApplication.class, args);
    }

}
