package com.qingqiu.openagent.util;

import com.qingqiu.openagent.service.EnumConfigFacadeService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 09:35
 * @description: EnumConfig helper —— 旧入口，保留兼容；新代码请直接注入 {@link EnumConfigFacadeService}
 * @deprecated 使用 {@link EnumConfigFacadeService}
 */
@Deprecated
@Component
@AllArgsConstructor
public class EnumConfigHelper {

    private final EnumConfigFacadeService enumConfigFacadeService;

    /** 获取指定类别下未禁用的枚举值列表。 */
    public List<String> getByType(String type) {
        return enumConfigFacadeService.listValues(type);
    }

    /** 强制刷新缓存。 */
    public void refresh() {
        enumConfigFacadeService.refresh();
    }
}
