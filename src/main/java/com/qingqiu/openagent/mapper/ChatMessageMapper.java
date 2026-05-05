package com.qingqiu.openagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingqiu.openagent.model.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author charon
 * @description 针对表【chat_message】的数据库操作Mapper
 * @createDate 2025-12-02 15:40:13
 * @Entity com.qingqiu.OpenAgent.model.entity.ChatMessage
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    List<ChatMessage> selectBySessionId(String sessionId);

    List<ChatMessage> selectBySessionIdRecently(String sessionId, int limit);
}
