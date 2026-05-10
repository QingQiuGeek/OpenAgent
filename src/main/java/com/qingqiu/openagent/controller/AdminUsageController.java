package com.qingqiu.openagent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qingqiu.openagent.model.common.R;
import com.qingqiu.openagent.model.entity.AgentUsageLog;
import com.qingqiu.openagent.model.vo.DailyUsageVO;
import com.qingqiu.openagent.model.vo.ModelUsageVO;
import com.qingqiu.openagent.model.vo.UsageOverviewVO;
import com.qingqiu.openagent.service.AgentUsageLogService;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 10:30
 * @description: Admin usage controller —— 路径前缀 /admin/ 由 SaTokenInterceptor 强制 admin 角色
 */
@RestController
@RequestMapping("/admin/usage")
@AllArgsConstructor
public class AdminUsageController {

    private final AgentUsageLogService agentUsageLogService;

    @GetMapping("/overview")
    public R<UsageOverviewVO> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.success(agentUsageLogService.overview(defaultFrom(from), defaultTo(to)));
    }

    @GetMapping("/daily")
    public R<List<DailyUsageVO>> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.success(agentUsageLogService.daily(defaultFrom(from), defaultTo(to)));
    }

    @GetMapping("/by-model")
    public R<List<ModelUsageVO>> byModel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.success(agentUsageLogService.groupByModel(defaultFrom(from), defaultTo(to)));
    }

    @GetMapping("/list")
    public R<IPage<AgentUsageLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status) {
        return R.success(agentUsageLogService.page(page, pageSize, defaultFrom(from), defaultTo(to), status));
    }

    private LocalDate defaultFrom(LocalDate from) {
        return from == null ? LocalDate.now().minusDays(7) : from;
    }

    private LocalDate defaultTo(LocalDate to) {
        return to == null ? LocalDate.now() : to;
    }
}
