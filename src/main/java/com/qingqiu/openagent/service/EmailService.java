package com.qingqiu.openagent.service;

/**
 * @author: qingqiugeek
 * @date: 2026/5/4 16:18
 * @description: Email service
 */
public interface EmailService {
    /**
     * 异步发送邮件
     *
     * @param to      收件人邮箱地址
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void sendEmailAsync(String to, String subject, String content);
}
