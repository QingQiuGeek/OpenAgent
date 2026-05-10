package com.qingqiu.openagent.controller;

import com.qingqiu.openagent.model.common.R;
import com.qingqiu.openagent.model.request.SubmitFeedbackRequest;
import com.qingqiu.openagent.model.vo.ChatFeedbackVO;
import com.qingqiu.openagent.service.ChatFeedbackFacadeService;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 12:15
 * @description: ChatFeedback controller
 */
@RestController
@RequestMapping("/api/chat-feedback")
@AllArgsConstructor
public class ChatFeedbackController {

    private final ChatFeedbackFacadeService chatFeedbackFacadeService;

    /** 提交或更新反馈 */
    @PostMapping
    public R<Long> submit(@RequestBody SubmitFeedbackRequest request) {
        return R.success(chatFeedbackFacadeService.submit(request));
    }

    /** 撤回反馈 */
    @DeleteMapping("/{messageId}")
    public R<Boolean> withdraw(@PathVariable("messageId") String messageId) {
        chatFeedbackFacadeService.withdraw(messageId);
        return R.success(true);
    }

    /** 单条查询当前用户对指定消息的反馈 */
    @GetMapping("/{messageId}")
    public R<ChatFeedbackVO> get(@PathVariable("messageId") String messageId) {
        return R.success(chatFeedbackFacadeService.get(messageId));
    }

    /** 批量查询：?messageIds=a,b,c */
    @GetMapping
    public R<List<ChatFeedbackVO>> batchGet(@RequestParam("messageIds") String messageIds) {
        if (messageIds == null || messageIds.isBlank()) {
            return R.success(List.of());
        }
        List<String> ids = Arrays.stream(messageIds.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return R.success(chatFeedbackFacadeService.batchGet(ids));
    }
}
