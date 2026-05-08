package com.qingqiu.openagent.controller;

import com.qingqiu.openagent.enums.ChatModeType;
import com.qingqiu.openagent.model.common.R;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: qingqiugeek
 * @date: 2026/5/1 16:54
 * @description: ProviderType controller
 */
@RestController
@RequestMapping("/api/provider-types")
public class ProviderTypeController {

    @GetMapping
    public R<List<ProviderTypeVO>> getProviderTypes() {
        List<ProviderTypeVO> list = Arrays.stream(ChatModeType.values())
                .map(v -> ProviderTypeVO.builder()
                        .code(v.getCode())
                        .description(v.getDescription())
                        .build())
                .collect(Collectors.toList());
        return R.success(list);
    }

    @Data
    @Builder
    public static class ProviderTypeVO {
        private String code;
        private String description;
    }
}
