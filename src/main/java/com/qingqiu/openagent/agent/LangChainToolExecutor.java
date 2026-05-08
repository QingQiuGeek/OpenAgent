package com.qingqiu.openagent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingqiu.openagent.agent.tools.ITool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: qingqiugeek
 * @date: 2026/5/2 17:27
 * @description: LangChainTool executor
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
            JsonNode valueNode = lookupArgument(argsNode, parameter, i);

            if (valueNode == null || valueNode.isNull()) {
                args[i] = null;
                continue;
            }

            args[i] = objectMapper.treeToValue(valueNode, parameter.getType());
        }
        return args;
    }

    /**
     * 按参数名（compiler 需 -parameters）查找 JSON 字段；如果取不到，回退到按位置取
     * （仅当字段顺序与参数顺序一致时生效），最后再尝试 argN / "arg{i}" 命名。
     * 不再使用 {@code @P.value()} 当字段名 —— 那是参数描述，会被 langchain4j 写入 JSON
     * Schema 的 {@code description}，不是 JSON key。
     */
    private JsonNode lookupArgument(JsonNode argsNode, Parameter parameter, int index) {
        // 1) 反射拿到的真实形参名
        String paramName = parameter.getName();
        JsonNode v = argsNode.get(paramName);
        if (v != null) return v;

        // 2) 兜底：按位置——argsNode 的第 i 个字段
        if (argsNode.isObject() && argsNode.size() > index) {
            Iterator<String> it = argsNode.fieldNames();
            int k = 0;
            while (it.hasNext()) {
                String name = it.next();
                if (k == index) {
                    return argsNode.get(name);
                }
                k++;
            }
        }

        // 3) 兜底：argN 命名（编译时未带 -parameters 的情况）
        return argsNode.get("arg" + index);
    }

    @FunctionalInterface
    private interface ToolMethodInvoker {
        String invoke(String argumentsJson);
    }
}
