package com.qingqiu.openagent.service;

import com.qingqiu.openagent.model.request.SubmitFeedbackRequest;
import com.qingqiu.openagent.model.vo.ChatFeedbackVO;
import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 12:05
 * @description: ChatFeedback facade service
 */
public interface ChatFeedbackFacadeService {

    /** 提交或更新反馈（基于 user_id+message_id 唯一） */
    Long submit(SubmitFeedbackRequest request);

    /** 撤回反馈 */
    void withdraw(String messageId);

    /** 单条查询：当前用户对某条消息的反馈，无则返回 null */
    ChatFeedbackVO get(String messageId);

    /** 批量查询：当前用户对一组消息的反馈 */
    List<ChatFeedbackVO> batchGet(List<String> messageIds);
}
