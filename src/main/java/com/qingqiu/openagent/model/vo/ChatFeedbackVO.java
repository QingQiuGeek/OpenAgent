package com.qingqiu.openagent.model.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 12:03
 * @description: ChatFeedback view object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatFeedbackVO {

    private Long id;
    private String messageId;
    private Short rating;
    private List<String> reasonTags;
    private String comment;
    private LocalDateTime createdAt;
}
