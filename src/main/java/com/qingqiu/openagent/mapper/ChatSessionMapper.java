package com.qingqiu.openagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingqiu.openagent.model.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author: qingqiugeek
 * @date: 2026/5/11 13:02
 * @description: ChatSession MyBatis mapper
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
