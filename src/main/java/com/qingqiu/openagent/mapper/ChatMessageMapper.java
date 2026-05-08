package com.qingqiu.openagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingqiu.openagent.model.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/1 08:08
 * @description: ChatMessage MyBatis mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    List<ChatMessage> selectBySessionId(String sessionId);

    List<ChatMessage> selectBySessionIdRecently(String sessionId, int limit);
}
