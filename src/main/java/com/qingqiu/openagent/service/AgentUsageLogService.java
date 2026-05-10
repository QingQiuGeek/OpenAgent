package com.qingqiu.openagent.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qingqiu.openagent.model.entity.AgentUsageLog;
import com.qingqiu.openagent.model.vo.DailyUsageVO;
import com.qingqiu.openagent.model.vo.ModelUsageVO;
import com.qingqiu.openagent.model.vo.UsageOverviewVO;
import java.time.LocalDate;
import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 10:05
 * @description: AgentUsageLog service
 */
public interface AgentUsageLogService {

    /** 异步落库一条调用日志，不影响业务流程。 */
    void recordAsync(AgentUsageLog entry);

    /** 全局总览（无 userId 过滤）。 */
    UsageOverviewVO overview(LocalDate from, LocalDate to);

    /** 按用户总览。 */
    UsageOverviewVO overviewByUser(Long userId, LocalDate from, LocalDate to);

    /** 按日聚合（折线图）。 */
    List<DailyUsageVO> daily(LocalDate from, LocalDate to);

    /** 按日聚合（当前用户）。 */
    List<DailyUsageVO> dailyByUser(Long userId, LocalDate from, LocalDate to);

    /** 按模型聚合。 */
    List<ModelUsageVO> groupByModel(LocalDate from, LocalDate to);

    /** 按模型聚合（当前用户）。 */
    List<ModelUsageVO> groupByModelByUser(Long userId, LocalDate from, LocalDate to);

    /** 分页明细（管理员）。 */
    IPage<AgentUsageLog> page(int page, int pageSize, LocalDate from, LocalDate to, String status);

    /** 分页明细（当前用户）。 */
    IPage<AgentUsageLog> pageByUser(Long userId, int page, int pageSize, LocalDate from, LocalDate to, String status);
}
