package com.qingqiu.openagent.service;

import com.qingqiu.openagent.model.entity.EnumConfig;
import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 09:31
 * @description: EnumConfig facade service —— DB 唯一权威字典源
 */
public interface EnumConfigFacadeService {

    /** 列出指定类别下未禁用的枚举值（仅 value，按 id 升序）。 */
    List<String> listValues(String type);

    /** 列出指定类别下的全部枚举项（含禁用，给管理员用）。 */
    List<EnumConfig> listAll(String type);

    /** 校验 value 是否在指定类别下且未禁用。 */
    boolean exists(String type, String value);

    /** 新增一项；type+value 已存在则抛 BizException。 */
    Long add(String type, String value);

    /** 更新启停状态：0 启用 / 1 禁用。 */
    void updateStatus(Long id, Integer status);

    /** 强制刷新内存缓存。CRUD 之后会自动调用。 */
    void refresh();
}
