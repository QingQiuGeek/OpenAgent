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
 * @author: qingqiugeek
 * @date: 2026/5/9 14:00
 * @description: ShareLink entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("share_link")
public class ShareLink {

    /** UUID 字符串 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private Long userId;

    /** chat_session.id */
    private String sessionId;

    /** 公开短码 */
    private String slug;

    /** JSON string：会话快照（创建瞬间的所有消息） */
    private String snapshot;

    /** NULL = 永不过期 */
    private LocalDateTime expireAt;

    private Integer viewCount;

    private LocalDateTime createdAt;

    @TableLogic
    private Integer isDeleted;
}
