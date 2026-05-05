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
 * @TableName agent_usage_log
 * <p>Agent 调用日志（Step 9 仅建表 + 实体 + Mapper，业务写入链路留待后续）。</p>
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

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Integer latencyMs;

    /** success / error / timeout */
    private String status;

    private String errorMsg;

    private LocalDateTime createdAt;
}
