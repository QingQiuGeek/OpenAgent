package com.qingqiu.openagent.model.response;

import com.qingqiu.openagent.model.vo.ChatMessageVO;
import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/5 19:05
 * @description: GetChatMessages response payload
 */

@Data
@Builder
public class GetChatMessagesResponse {
    private ChatMessageVO[] chatMessages;
}

