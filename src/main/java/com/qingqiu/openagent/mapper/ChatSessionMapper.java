package com.qingqiu.openagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingqiu.openagent.model.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * ChatSession Mapper。基于 MyBatis-Plus，所有查询通过 {@code LambdaQueryWrapper} 叠加
 * {@code user_id} 条件实现多租户隔离。
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
