package com.qingqiu.openagent.model.response;

import com.qingqiu.openagent.model.vo.ChatSessionVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetChatSessionsResponse {
    private ChatSessionVO[] chatSessions;
}
