package com.qingqiu.openagent.agent.tools;

import com.qingqiu.openagent.service.EmailService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EmailTools implements ITool {

    private final EmailService emailService;

    public EmailTools(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public String getName() {
        return "emailTool";
    }

    @Override
    public String getDescription() {
        return "Send email asynchronously";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @Tool(name = "sendEmail", value = "Send email with to, subject, content")
    public String sendEmail(String to, String subject, String content) {
        if (!StringUtils.hasText(to) || !StringUtils.hasText(subject) || !StringUtils.hasText(content)) {
            return "Error: to, subject and content are required.";
        }
        emailService.sendEmailAsync(to, subject, content);
        return "Email task submitted.";
    }
}
