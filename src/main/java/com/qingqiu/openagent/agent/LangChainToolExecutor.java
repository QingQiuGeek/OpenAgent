package com.qingqiu.openagent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingqiu.openagent.agent.tools.ITool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import org.springframework.aop.support.AopUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 LangChain4j 的 ToolExecutionRequest 映射到 Spring Bean 工具方法执行。
 */
public class LangChainToolExecutor {

    private final ObjectMapper objectMapper;
    private final List<ToolSpecification> toolSpecifications;
    private final Map<String, ToolMethodInvoker> invokerByToolName;

    public LangChainToolExecutor(List<ITool> iTools, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.toolSpecifications = new ArrayList<>();
        this.invokerByToolName = new HashMap<>();
        init(iTools);
    }

    public List<ToolSpecification> getToolSpecifications() {
        return toolSpecifications;
    }

    public String execute(ToolExecutionRequest request) {
        ToolMethodInvoker invoker = invokerByToolName.get(request.name());
        if (invoker == null) {
            throw new IllegalArgumentException("未找到工具: " + request.name());
        }
        return invoker.invoke(request.arguments());
    }

    private void init(List<ITool> iTools) {
        for (ITool iTool : iTools) {
            toolSpecifications.addAll(ToolSpecifications.toolSpecificationsFrom(iTool));
            registerInvokers(iTool);
        }
    }

    private void registerInvokers(ITool iTool) {
        Class<?> targetClass = AopUtils.getTargetClass(iTool);
        for (Method method : targetClass.getDeclaredMethods()) {
            dev.langchain4j.agent.tool.Tool toolAnnotation = method.getAnnotation(dev.langchain4j.agent.tool.Tool.class);
            if (toolAnnotation == null) {
                continue;
            }

            String toolName = toolAnnotation.name();
            if (toolName == null || toolName.isBlank()) {
                toolName = method.getName();
            }
            final String resolvedToolName = toolName;

            method.setAccessible(true);
            invokerByToolName.put(resolvedToolName, argumentsJson -> {
                try {
                    Object[] args = resolveArguments(method, argumentsJson);
                    Object result = method.invoke(iTool, args);
                    if (result == null) {
                        return "";
                    }
                    if (result instanceof String s) {
                        return s;
                    }
                    return objectMapper.writeValueAsString(result);
                } catch (Exception e) {
                    throw new RuntimeException("执行工具失败: " + resolvedToolName, e);
                }
            });
        }
    }

    private Object[] resolveArguments(Method method, String argumentsJson) throws Exception {
        Parameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            return new Object[0];
        }

        JsonNode argsNode = (argumentsJson == null || argumentsJson.isBlank())
                ? objectMapper.createObjectNode()
                : objectMapper.readTree(argumentsJson);

        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String paramName = resolveParameterName(parameter);
            JsonNode valueNode = argsNode.get(paramName);

            if (valueNode == null || valueNode.isNull()) {
                args[i] = null;
                continue;
            }

            args[i] = objectMapper.treeToValue(valueNode, parameter.getType());
        }
        return args;
    }

    private String resolveParameterName(Parameter parameter) {
        for (Annotation annotation : parameter.getAnnotations()) {
            if (annotation.annotationType() == P.class) {
                P p = (P) annotation;
                if (p.value() != null && !p.value().isBlank()) {
                    return p.value();
                }
            }
        }
        return parameter.getName();
    }

    @FunctionalInterface
    private interface ToolMethodInvoker {
        String invoke(String argumentsJson);
    }
}
