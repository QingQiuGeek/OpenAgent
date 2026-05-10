package com.qingqiu.openagent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import com.qingqiu.openagent.model.common.R;
import com.qingqiu.openagent.model.entity.AgentUsageLog;
import com.qingqiu.openagent.model.vo.DailyUsageVO;
import com.qingqiu.openagent.model.vo.ModelUsageVO;
import com.qingqiu.openagent.model.vo.UsageOverviewVO;
import com.qingqiu.openagent.service.AgentUsageLogService;
import com.qingqiu.openagent.util.UserContext;
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
 * @date: 2026/5/10 21:30
 * @description: 用户视角的用量看板：所有接口强制按当前登录 userId 过滤。
 */
@RestController
@RequestMapping("/api/usage")
@AllArgsConstructor
public class UsageController {

    private final AgentUsageLogService agentUsageLogService;

    @GetMapping("/overview")
    public R<UsageOverviewVO> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.success(agentUsageLogService.overviewByUser(currentUser(), defaultFrom(from), defaultTo(to)));
    }

    @GetMapping("/daily")
    public R<List<DailyUsageVO>> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.success(agentUsageLogService.dailyByUser(currentUser(), defaultFrom(from), defaultTo(to)));
    }

    @GetMapping("/by-model")
    public R<List<ModelUsageVO>> byModel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.success(agentUsageLogService.groupByModelByUser(currentUser(), defaultFrom(from), defaultTo(to)));
    }

    @GetMapping("/list")
    public R<IPage<AgentUsageLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status) {
        return R.success(agentUsageLogService.pageByUser(
                currentUser(), page, pageSize, defaultFrom(from), defaultTo(to), status));
    }

    private Long currentUser() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizException(BizExceptionEnum.NOT_LOGIN_ERROR.getCode(),
                    BizExceptionEnum.NOT_LOGIN_ERROR.getMessage());
        }
        return userId;
    }

    private LocalDate defaultFrom(LocalDate from) {
        return from == null ? LocalDate.now().minusDays(7) : from;
    }

    private LocalDate defaultTo(LocalDate to) {
        return to == null ? LocalDate.now() : to;
    }
}
