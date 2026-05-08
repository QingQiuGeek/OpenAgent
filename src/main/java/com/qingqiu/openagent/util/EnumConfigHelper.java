package com.qingqiu.openagent.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingqiu.openagent.mapper.EnumConfigMapper;
import com.qingqiu.openagent.model.entity.EnumConfig;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author: qingqiugeek
 * @date: 2026/5/1 15:15
 * @description: EnumConfigHelper
 */
@Component
@AllArgsConstructor
public class EnumConfigHelper {

    private final EnumConfigMapper enumConfigMapper;

    private final ConcurrentHashMap<String, List<EnumConfig>> cache = new ConcurrentHashMap<>();

    /** 获取指定类别下未禁用的枚举项，按 sort 升序。 */
    public List<EnumConfig> getByType(String typeCode) {
        return cache.computeIfAbsent(typeCode, this::loadByType);
    }

    public void refresh() {
        cache.clear();
    }

    private List<EnumConfig> loadByType(String typeCode) {
        LambdaQueryWrapper<EnumConfig> qw = new LambdaQueryWrapper<>();
        qw.eq(EnumConfig::getTypeCode, typeCode)
                .eq(EnumConfig::getStatus, 0)
                .orderByAsc(EnumConfig::getSort);
        return enumConfigMapper.selectList(qw);
    }
}
