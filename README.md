## TODO

正常功能测试
- ai接入，对话测试 √
- 富文本渲染 √
- 头像修改，聊天字体加大 √
- 来源引用 √
- 文件上传 读docx、pdf等文件√
- 文生图 √
- pdf生成工具
- 图片url有效期，上传到oss
- 输入框 mcp配置
- A2A

# AI智能体助手

智能 AI Agent 系统，基于 LangChain4j 框架构建，实现了自主决策、工具调用和知识库检索等核心能力。

系统采用 **Think-Execute 循环机制，能够理解复杂任务、规划执行步骤、调用外部工具，并基于 RAG 技术从知识库中检索相关信息，完成多步骤的复杂任务**。

它不是“聊天机器人”，而是 Agent：**能规划、能调用工具、能检索知识库、还能把执行过程实时推给前端**。


1、**真正的 Agent Loop（Think-Execute 循环 + 状态机**）

- 多轮规划
- 多轮工具调用
- 状态管理（THINKING / EXECUTING / DONE / ERROR）
- 错误处理与最大步数控制（防止无限循环）

2、**工具系统（固定工具 + 可选工具，可扩展、可治理**）

- 工具自动注册
- 固定工具 / 可选工具分类管理
- 可扩展：新增工具不改核心流程
- 可控：禁用框架自动执行，改为手动管理 ToolCalling 流程

3、**RAG 知识库（PostgreSQL + pgvector**）

- Markdown 文档解析、分块
- Embedding 生成并落库
- pgvector 相似度检索（<->）
- ivfflat 索引优化，支持 10 万+向量

4、**多模型支持（注册表模式 ChatClientRegistry**）

- DeepSeek / 智谱 AI 可切换
- 统一 ChatModel 接口
- 注册表模式管理模型实例（解耦创建与使用）
- 便于未来扩展更多模型

5、**SSE 实时通信（执行过程实时可视化**）

- 状态实时推送：THINKING / EXECUTING / DONE
- 前端能实时看到“Agent 正在干啥”
