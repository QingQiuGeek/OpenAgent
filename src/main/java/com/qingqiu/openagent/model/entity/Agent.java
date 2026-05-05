package com.qingqiu.openagent.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @TableName agent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent")
public class Agent {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属用户 id，所有查询按此字段做租户隔离。 */
    private Long userId;

    private String name;

    private String description;

    private String systemPrompt;

    /** 关联 {@link Model#getId()}，允许为空（模型被删除时 FK 置 null）。 */
    private Long modelId;

    /** JSON string：允许使用的工具列表（含 {@code mcp:{id}} 引用）。 */
    private String allowedTools;

    /** JSON string：允许访问的知识库 id 列表。 */
    private String allowedKbs;

    /** JSON string：聊天参数（temperature/topP/messageLength）。 */
    private String chatOptions;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}