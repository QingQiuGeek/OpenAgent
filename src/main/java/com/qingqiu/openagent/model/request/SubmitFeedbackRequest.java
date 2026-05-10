package com.qingqiu.openagent.model.request;

import java.util.List;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 12:02
 * @description: Submit feedback request payload
 */
@Data
public class SubmitFeedbackRequest {

    /** chat_message.id */
    private String messageId;

    /** 1 赞 / -1 踩 */
    private Short rating;

    private List<String> reasonTags;

    private String comment;
}
