package com.qingqiu.openagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import com.qingqiu.openagent.mapper.EnumConfigMapper;
import com.qingqiu.openagent.model.entity.EnumConfig;
import com.qingqiu.openagent.service.EnumConfigFacadeService;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 09:32
 * @description: EnumConfig facade service implementation
 */
@Slf4j
@Service
@AllArgsConstructor
public class EnumConfigFacadeServiceImpl implements EnumConfigFacadeService {

    private final EnumConfigMapper enumConfigMapper;

    /** 内存缓存：type → enabled values。修改后整体重建。 */
    private final ConcurrentHashMap<String, List<String>> enabledCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    @Override
    public synchronized void refresh() {
        List<EnumConfig> all = enumConfigMapper.selectList(
                new LambdaQueryWrapper<EnumConfig>().eq(EnumConfig::getStatus, 0).orderByAsc(EnumConfig::getId));
        Map<String, List<String>> grouped = all.stream().collect(
                Collectors.groupingBy(EnumConfig::getType,
                        Collectors.mapping(EnumConfig::getValue, Collectors.toList())));
        enabledCache.clear();
        enabledCache.putAll(grouped);
        log.info("[EnumConfig] cache refreshed, types={} totalItems={}", enabledCache.keySet(), all.size());
    }

    @Override
    public List<String> listValues(String type) {
        return enabledCache.getOrDefault(type, Collections.emptyList());
    }

    @Override
    public List<EnumConfig> listAll(String type) {
        return enumConfigMapper.selectList(
                new LambdaQueryWrapper<EnumConfig>().eq(EnumConfig::getType, type).orderByAsc(EnumConfig::getId));
    }

    @Override
    public boolean exists(String type, String value) {
        if (type == null || value == null) return false;
        List<String> values = enabledCache.get(type);
        return values != null && values.contains(value);
    }

    @Override
    public Long add(String type, String value) {
        if (type == null || type.isBlank() || value == null || value.isBlank()) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "type / value 不能为空");
        }
        Long count = enumConfigMapper.selectCount(new LambdaQueryWrapper<EnumConfig>()
                .eq(EnumConfig::getType, type).eq(EnumConfig::getValue, value));
        if (count != null && count > 0) {
            throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "该枚举项已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        EnumConfig entity = EnumConfig.builder()
                .type(type)
                .value(value)
                .status(0)
                .isDeleted(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        enumConfigMapper.insert(entity);
        refresh();
        return entity.getId();
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (id == null) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "id 不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "status 必须为 0 或 1");
        }
        EnumConfig entity = enumConfigMapper.selectById(id);
        if (entity == null) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(), "枚举项不存在: " + id);
        }
        entity.setStatus(status);
        entity.setUpdatedAt(LocalDateTime.now());
        enumConfigMapper.updateById(entity);
        refresh();
    }
}
