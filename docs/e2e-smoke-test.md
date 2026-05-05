# OpenAgent 多租户改造 端到端回归脚本

本脚本用于手工在 Knife4j (`/doc.html`) 或 curl 下验证改造后的接口是否满足多租户隔离与 modelId 绑定要求。运行前请确保：

1. 已执行 `src/main/resources/db/migration/V2__multi_tenant_and_dynamic_model.sql`（或用全新 `openagent.sql` 初始化）。
2. `application.yml` 中 deepseek / zhipuai 的 yml fallback 仍然可用（或 mock 一个）。
3. 启动应用：`./mvnw spring-boot:run`。

> 下文 `$TOKEN_A` / `$TOKEN_B` 需要用两个邮箱跑完注册 + 登录后替换。

## 0. 健康检查
```bash
curl -i http://localhost:8080/error       # 期望 404/500 空体
curl -i http://localhost:8080/api/agents  # 期望 401 未登录（NOT_LOGIN_ERROR）
```

## 1. 两个用户注册 + 登录
```bash
# A: 请求验证码（若邮件服务未配置，测试时请手动改用 db 里种一条数据）
curl -X POST http://localhost:8080/user/register-code \
  -H "Content-Type: application/json" \
  -d '{"mail":"a@example.com"}'

# 从日志/邮件里拿到 $CODE_A
curl -X POST http://localhost:8080/user/register \
  -H "Content-Type: application/json" \
  -d '{"mail":"a@example.com","password":"Abcdef1!","rePassword":"Abcdef1!","code":"$CODE_A"}'

# 登录取 tokenValue
curl -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{"mail":"a@example.com","password":"Abcdef1!"}'
# → 拿到 $TOKEN_A

# B 同理 → $TOKEN_B
```

## 2. A 创建模型
```bash
curl -X POST http://localhost:8080/api/models \
  -H "satoken: $TOKEN_A" -H "Content-Type: application/json" \
  -d '{"modelName":"deepseek-chat","providerType":"openai","baseUrl":"https://api.deepseek.com/v1","apiKey":"sk-xxx","maxTokens":8000}'
# → 拿到 $MODEL_A_ID

curl http://localhost:8080/api/models -H "satoken: $TOKEN_A"     # A 看到 1 条
curl http://localhost:8080/api/models -H "satoken: $TOKEN_B"     # B 看到 0 条（隔离OK）
```

## 3. A 创建 Agent（绑定 modelId）
```bash
curl -X POST http://localhost:8080/api/agents \
  -H "satoken: $TOKEN_A" -H "Content-Type: application/json" \
  -d '{
    "name":"我的 DS Agent",
    "description":"demo",
    "systemPrompt":"你是一个助手",
    "modelId": '$MODEL_A_ID',
    "allowedTools":[],
    "allowedKbs":[],
    "chatOptions":{"temperature":0.7,"topP":1.0,"messageLength":10}
  }'
# → $AGENT_A_ID
```

## 4. B 尝试用 A 的 model 创建 agent（应被拒绝）
```bash
curl -X POST http://localhost:8080/api/agents \
  -H "satoken: $TOKEN_B" -H "Content-Type: application/json" \
  -d '{
    "name":"盗号尝试",
    "description":"demo",
    "systemPrompt":"x",
    "modelId": '$MODEL_A_ID',
    "allowedTools":[],"allowedKbs":[],
    "chatOptions":{"temperature":0.7,"topP":1.0,"messageLength":10}
  }'
# 期望：FORBIDDEN_ERROR "无权使用该模型: $MODEL_A_ID"
```

## 5. B 看不到 A 的 Agent
```bash
curl http://localhost:8080/api/agents -H "satoken: $TOKEN_B"  # 返回空数组
curl -X PATCH http://localhost:8080/api/agents/$AGENT_A_ID \
  -H "satoken: $TOKEN_B" -H "Content-Type: application/json" \
  -d '{"name":"hack"}'   # 期望 FORBIDDEN_ERROR
curl -X DELETE http://localhost:8080/api/agents/$AGENT_A_ID \
  -H "satoken: $TOKEN_B" # 期望 FORBIDDEN_ERROR
```

## 6. A 创建 KB + 上传文档 + 聊天
```bash
curl -X POST http://localhost:8080/api/knowledge-bases \
  -H "satoken: $TOKEN_A" -H "Content-Type: application/json" \
  -d '{"name":"test-kb","description":"demo"}'
# → $KB_A_ID

# 上传 md
curl -X POST http://localhost:8080/api/documents/upload/$KB_A_ID \
  -H "satoken: $TOKEN_A" -F "file=@README.md"

# 创建会话
curl -X POST http://localhost:8080/api/chat-sessions \
  -H "satoken: $TOKEN_A" -H "Content-Type: application/json" \
  -d '{"agentId":"'$AGENT_A_ID'","title":"demo"}'
# → $SESSION_A_ID

# 发消息（触发 agent + DynamicChatModelService）
curl -X POST http://localhost:8080/api/chat-messages \
  -H "satoken: $TOKEN_A" -H "Content-Type: application/json" \
  -d '{"sessionId":"'$SESSION_A_ID'","agentId":"'$AGENT_A_ID'","role":"user","content":"你好"}'
```

## 7. B 跨租户访问 A 的 KB/Session/Doc（全部应被拒绝）
```bash
curl http://localhost:8080/api/knowledge-bases -H "satoken: $TOKEN_B"                # 0 条
curl http://localhost:8080/api/chat-sessions   -H "satoken: $TOKEN_B"                # 0 条
curl http://localhost:8080/api/chat-messages/session/$SESSION_A_ID -H "satoken: $TOKEN_B"  # FORBIDDEN_ERROR
curl http://localhost:8080/api/documents/kb/$KB_A_ID -H "satoken: $TOKEN_B"          # FORBIDDEN_ERROR
```

## 8. MCP Server 注册
```bash
curl -X POST http://localhost:8080/api/mcp-servers \
  -H "satoken: $TOKEN_A" -H "Content-Type: application/json" \
  -d '{"name":"local-fs","transport":"stdio","command":"npx -y @modelcontextprotocol/server-filesystem .","enabled":1}'

curl http://localhost:8080/api/mcp-servers -H "satoken: $TOKEN_B"  # 0 条
```

## 9. 动态模型失效回退 yml
```bash
# A 删除 model
curl -X DELETE http://localhost:8080/api/models/$MODEL_A_ID -H "satoken: $TOKEN_A"

# A 继续用旧 agent 发消息 → DynamicChatModelService.resolve(modelId, "deepseek-chat") 走 yml 兜底
curl -X POST http://localhost:8080/api/chat-messages \
  -H "satoken: $TOKEN_A" -H "Content-Type: application/json" \
  -d '{"sessionId":"'$SESSION_A_ID'","agentId":"'$AGENT_A_ID'","role":"user","content":"fallback ok?"}'
```

## 10. 拦截器白名单确认
```bash
curl -i http://localhost:8080/doc.html                    # 200
curl -i http://localhost:8080/v3/api-docs                  # 200
curl -i http://localhost:8080/api/agents                   # 401
curl -i http://localhost:8080/sse/connect/fake-session-id  # 401（SSE 也会拦截）
```

---

**通过标准**：上面每一步的返回结果都与注释一致；任何一步失败立即停止并回滚到上一步 commit。
