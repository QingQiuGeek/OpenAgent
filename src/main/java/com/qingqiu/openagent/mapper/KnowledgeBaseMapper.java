package com.qingqiu.openagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingqiu.openagent.model.entity.KnowledgeBase;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * KnowledgeBase Mapper。批量查询通过 {@link #selectBatchIds} 实现（MP 原生方法）。
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    /** 兼容原有调用：按 id 列表批量查询。直接代理到 MP {@code selectBatchIds}。 */
    default List<KnowledgeBase> selectByIdBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectBatchIds(ids);
    }
}
