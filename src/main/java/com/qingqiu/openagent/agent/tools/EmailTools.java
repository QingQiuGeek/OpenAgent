package com.qingqiu.openagent.agent.tools;

import com.qingqiu.openagent.service.EmailService;
import dev.langchain4j.agent.tool.P;
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

    @Tool(name = "sendEmail", value = "Send an email asynchronously with the given recipient, subject and plain text content")
    public String sendEmail(
            @P(value = "Recipient email address, e.g. someone@example.com")
            String to,
            @P(value = "Email subject line, short and informative")
            String subject,
            @P(value = "Email body in plain text")
            String content) {
        if (!StringUtils.hasText(to) || !StringUtils.hasText(subject) || !StringUtils.hasText(content)) {
            return "Error: to, subject and content are required.";
        }
        emailService.sendEmailAsync(to, subject, content);
        return "Email task submitted.";
    }
}
