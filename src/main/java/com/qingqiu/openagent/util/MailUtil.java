package com.qingqiu.openagent.util;

import static com.qingqiu.openagent.constant.Common.REGISTER_CAPTCHA_TTL;

import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * @author: qingqiugeek
 * @date: 2026/5/10 08:53
 * @description: Mail utility class
 */
@Slf4j
public class MailUtil {

    private final JavaMailSenderImpl mailSender;
    private final String fromEmail;

    public MailUtil(JavaMailSenderImpl mailSender, String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    /**
     * 向目标邮箱发送 6 位注册验证码。
     */
    public void sendCode(String toMail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toMail);
        message.setSubject("OpenAgent 注册验证码");
        message.setText("验证码：" + code + "/n"
            + REGISTER_CAPTCHA_TTL +" 分钟内有效。若非本人操作请忽略。");
        try {
            mailSender.send(message);
        } catch (RuntimeException e) {
            log.error("发送邮件失败, mail={}", toMail, e);
            throw new BizException(BizExceptionEnum.SYSTEM_BUSY.getCode(), "邮件发送失败，请稍后重试");
        }
    }
}
