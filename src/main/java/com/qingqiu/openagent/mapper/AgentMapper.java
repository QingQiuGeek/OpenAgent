package com.qingqiu.openagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingqiu.openagent.model.entity.Agent;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent Mapper。继承 MyBatis-Plus {@link BaseMapper}，所有标准 CRUD 自动可用。
 */
@Mapper
public interface AgentMapper extends BaseMapper<Agent> {
}
