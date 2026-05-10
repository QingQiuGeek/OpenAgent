package com.qingqiu.openagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author: qingqiugeek
 * @date: 2026/5/6 17:37
 * @description: OpenAgent Spring Boot application entry
 */

@SpringBootApplication
@EnableAsync
@MapperScan("com.qingqiu.openagent.mapper")
public class OpenAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenAgentApplication.class, args);
    }

}
