package com.qingqiu.openagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.qingqiu.openagent.mapper")
public class OpenAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenAgentApplication.class, args);
    }

}
