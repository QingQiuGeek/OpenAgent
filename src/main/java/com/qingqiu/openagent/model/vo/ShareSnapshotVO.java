package com.qingqiu.openagent.model.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 14:04
 * @description: 公开分享页要展示的会话快照
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareSnapshotVO {

    private String slug;

    /** 创建分享时的会话标题 */
    private String title;

    /** 创建分享时的 Agent 名称（可选） */
    private String agentName;

    /** 已过滤的消息数组（结构尽量与前端 ChatMessageVO 对齐），交给前端按 markdown 渲染 */
    private JsonNode messages;

    private LocalDateTime createdAt;
    private Integer viewCount;
}
