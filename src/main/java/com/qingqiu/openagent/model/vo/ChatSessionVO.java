package com.qingqiu.openagent.model.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatSessionVO {
    private String id;
    private Long userId;
    private String agentId;
    private String title;
    private LocalDateTime createdAt;
}
