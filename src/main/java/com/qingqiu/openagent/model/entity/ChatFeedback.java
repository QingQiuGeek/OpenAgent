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
 * @date: 2026/5/9 12:00
 * @description: ChatFeedback entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_feedback")
public class ChatFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** chat_message.id (UUID 字符串) */
    private String messageId;

    /** 1 赞 / -1 踩 */
    private Short rating;

    /** JSON string: ["不准确","格式差"] */
    private String reasonTags;

    private String comment;

    private LocalDateTime createdAt;
}
