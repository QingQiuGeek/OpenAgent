package com.qingqiu.openagent.controller;

import com.qingqiu.openagent.service.SseService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 11:00
 * @description: Test controller
 */

@RestController
@AllArgsConstructor
public class TestController {

    private final SseService sseService;

    @RequestMapping("/health")
    public String health() {
        return "ok";
    }

    @GetMapping("/sse-test")
    public String sseTest() {
        return "ok";
    }
}
