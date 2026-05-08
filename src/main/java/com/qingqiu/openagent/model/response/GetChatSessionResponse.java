package com.qingqiu.openagent.model.response;

import com.qingqiu.openagent.model.vo.ChatSessionVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/6 22:42
 * @description: GetChatSession response payload
 */

@Data
@AllArgsConstructor
@Builder
public class GetChatSessionResponse {
    private ChatSessionVO chatSession;
}
