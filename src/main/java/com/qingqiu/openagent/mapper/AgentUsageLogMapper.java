package com.qingqiu.openagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingqiu.openagent.model.entity.AgentUsageLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * agent_usage_log Mapper。Step 9 仅预埋接口，后续由 {@code ChatAgent} 流程在请求闭环时写入。
 */
@Mapper
public interface AgentUsageLogMapper extends BaseMapper<AgentUsageLog> {
}
