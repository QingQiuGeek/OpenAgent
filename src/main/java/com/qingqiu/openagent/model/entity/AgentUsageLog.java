package com.qingqiu.openagent.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: qingqiugeek
 * @date: 2026/5/5 13:47
 * @description: AgentUsageLog
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_usage_log")
public class AgentUsageLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String agentId;

    private String sessionId;

    private Long modelId;

    /** normal / agent / web_search ...（前端发起对话的模式） */
    private String chatMode;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Integer latencyMs;

    /** success / error / timeout */
    private String status;

    private String errorMsg;

    private LocalDateTime createdAt;
}
