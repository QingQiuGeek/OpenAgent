package com.qingqiu.openagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingqiu.openagent.model.entity.Agent;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author: qingqiugeek
 * @date: 2026/5/5 12:48
 * @description: Agent MyBatis mapper
 */
@Mapper
public interface AgentMapper extends BaseMapper<Agent> {
}
