package com.qingqiu.openagent.model.response;

import com.qingqiu.openagent.model.vo.ChatSessionVO;
import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 08:29
 * @description: GetChatSessions response payload
 */

@Data
@Builder
public class GetChatSessionsResponse {
    private ChatSessionVO[] chatSessions;
}
