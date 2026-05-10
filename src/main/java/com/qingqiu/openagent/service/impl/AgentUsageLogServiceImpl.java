package com.qingqiu.openagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qingqiu.openagent.mapper.AgentUsageLogMapper;
import com.qingqiu.openagent.mapper.ModelMapper;
import com.qingqiu.openagent.model.entity.AgentUsageLog;
import com.qingqiu.openagent.model.entity.Model;
import com.qingqiu.openagent.model.vo.DailyUsageVO;
import com.qingqiu.openagent.model.vo.ModelUsageVO;
import com.qingqiu.openagent.model.vo.UsageOverviewVO;
import com.qingqiu.openagent.service.AgentUsageLogService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 10:08
 * @description: AgentUsageLog service implementation
 */
@Slf4j
@Service
@AllArgsConstructor
public class AgentUsageLogServiceImpl implements AgentUsageLogService {

    private final AgentUsageLogMapper agentUsageLogMapper;
    private final ModelMapper modelMapper;

    @Override
    @Async
    public void recordAsync(AgentUsageLog entry) {
        try {
            if (entry.getCreatedAt() == null) {
                entry.setCreatedAt(LocalDateTime.now());
            }
            agentUsageLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("[AgentUsageLog] async record failed: {}", e.getMessage());
        }
    }

    @Override
    public UsageOverviewVO overview(LocalDate from, LocalDate to) {
        return computeOverview(buildRangeQuery(null, from, to));
    }

    @Override
    public UsageOverviewVO overviewByUser(Long userId, LocalDate from, LocalDate to) {
        return computeOverview(buildRangeQuery(userId, from, to));
    }

    @Override
    public List<DailyUsageVO> daily(LocalDate from, LocalDate to) {
        return dailyByUser(null, from, to);
    }

    @Override
    public List<DailyUsageVO> dailyByUser(Long userId, LocalDate from, LocalDate to) {
        List<AgentUsageLog> list = agentUsageLogMapper.selectList(buildRangeQuery(userId, from, to));
        TreeMap<String, long[]> byDate = new TreeMap<>();   // date -> [calls, totalTokens]
        for (AgentUsageLog l : list) {
            if (l.getCreatedAt() == null) continue;
            String d = l.getCreatedAt().toLocalDate().toString();
            long[] arr = byDate.computeIfAbsent(d, k -> new long[]{0, 0});
            arr[0]++;
            arr[1] += l.getTotalTokens() == null ? 0 : l.getTotalTokens();
        }
        return byDate.entrySet().stream()
                .map(e -> DailyUsageVO.builder()
                        .date(e.getKey())
                        .calls(e.getValue()[0])
                        .totalTokens(e.getValue()[1])
                        .build())
                .toList();
    }

    @Override
    public List<ModelUsageVO> groupByModel(LocalDate from, LocalDate to) {
        return groupByModelByUser(null, from, to);
    }

    @Override
    public List<ModelUsageVO> groupByModelByUser(Long userId, LocalDate from, LocalDate to) {
        List<AgentUsageLog> list = agentUsageLogMapper.selectList(buildRangeQuery(userId, from, to));
        Map<Long, long[]> agg = new HashMap<>(); // modelId -> [calls, tokens, latencySum, errors]
        for (AgentUsageLog l : list) {
            Long mid = l.getModelId();
            if (mid == null) continue;
            long[] arr = agg.computeIfAbsent(mid, k -> new long[]{0, 0, 0, 0});
            arr[0]++;
            arr[1] += l.getTotalTokens() == null ? 0 : l.getTotalTokens();
            arr[2] += l.getLatencyMs() == null ? 0 : l.getLatencyMs();
            if ("error".equalsIgnoreCase(l.getStatus())) arr[3]++;
        }
        // 取 modelName
        Map<Long, String> nameMap = new HashMap<>();
        if (!agg.isEmpty()) {
            List<Model> models = modelMapper.selectBatchIds(agg.keySet());
            for (Model m : models) nameMap.put(m.getId(), m.getModelName());
        }
        return agg.entrySet().stream().map(e -> {
            long[] v = e.getValue();
            return ModelUsageVO.builder()
                    .modelId(e.getKey())
                    .modelName(nameMap.getOrDefault(e.getKey(), "model#" + e.getKey()))
                    .calls(v[0])
                    .totalTokens(v[1])
                    .avgLatencyMs(v[0] == 0 ? 0 : v[2] / v[0])
                    .errorCalls(v[3])
                    .build();
        }).toList();
    }

    @Override
    public IPage<AgentUsageLog> page(int page, int pageSize, LocalDate from, LocalDate to, String status) {
        return pageByUser(null, page, pageSize, from, to, status);
    }

    @Override
    public IPage<AgentUsageLog> pageByUser(Long userId, int page, int pageSize, LocalDate from, LocalDate to, String status) {
        LambdaQueryWrapper<AgentUsageLog> qw = buildRangeQuery(userId, from, to);
        if (status != null && !status.isBlank()) {
            qw.eq(AgentUsageLog::getStatus, status);
        }
        qw.orderByDesc(AgentUsageLog::getCreatedAt);
        return agentUsageLogMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    // ------- helpers -------

    private LambdaQueryWrapper<AgentUsageLog> buildRangeQuery(Long userId, LocalDate from, LocalDate to) {
        LambdaQueryWrapper<AgentUsageLog> qw = new LambdaQueryWrapper<>();
        if (userId != null) qw.eq(AgentUsageLog::getUserId, userId);
        if (from != null) qw.ge(AgentUsageLog::getCreatedAt, from.atStartOfDay());
        if (to != null) qw.lt(AgentUsageLog::getCreatedAt, to.plusDays(1).atStartOfDay());
        return qw;
    }

    private UsageOverviewVO computeOverview(LambdaQueryWrapper<AgentUsageLog> qw) {
        List<AgentUsageLog> list = agentUsageLogMapper.selectList(qw);
        long total = list.size();
        long tokens = 0, prompt = 0, completion = 0, latencySum = 0, errors = 0;
        int latencyCount = 0;
        for (AgentUsageLog l : list) {
            tokens += l.getTotalTokens() == null ? 0 : l.getTotalTokens();
            prompt += l.getPromptTokens() == null ? 0 : l.getPromptTokens();
            completion += l.getCompletionTokens() == null ? 0 : l.getCompletionTokens();
            if (l.getLatencyMs() != null) {
                latencySum += l.getLatencyMs();
                latencyCount++;
            }
            if ("error".equalsIgnoreCase(l.getStatus())) errors++;
        }
        return UsageOverviewVO.builder()
                .totalCalls(total)
                .totalTokens(tokens)
                .totalPromptTokens(prompt)
                .totalCompletionTokens(completion)
                .avgLatencyMs(latencyCount == 0 ? 0 : latencySum / latencyCount)
                .errorCalls(errors)
                .errorRate(total == 0 ? 0.0 : (double) errors / total)
                .build();
    }
}
