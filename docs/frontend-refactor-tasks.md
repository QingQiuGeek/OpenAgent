# 前端重构任务清单

> 背景：后端新增 `model` / `mcp_server` / `user` 相关表，`AgentVO` 由 `model: string` 改为 `modelId: Long` + `modelName: string`，并启用多租户隔离。前端需要对齐，并补登录/注册、自定义模型、侧边栏折叠等功能。
> 原则：**只改逻辑打通联调，不动现有 UI 风格**；新增 UI 仅限任务 1/2/3 明确要求的部分。

---

## 前提勘误

前端 `src/api/api.ts` 仍使用旧的 `model: ModelType` 字段，后端接口已要求 `modelId: Long`。**在动其他改造前必须先把此字段对齐**，否则 Agent 创建/更新直接 400。

---

## Phase 1 — 登录认证（支撑任务 1 / 任务 4）

| # | 文件 | 动作 | 状态 |
|---|---|---|---|
| 1.1 | **后端** `ProviderTypeController`（新建） + `GlobalExceptionHandler` 保留 BizException code + 处理 `NotLoginException` | 新增 `GET /api/provider-types` | ✅ |
| 1.2 | `src/api/http.ts` | 自动注入 `authorization`；`40200` 触发 `auth:unauthorized` 全局事件 | ✅ |
| 1.3 | `src/api/api.ts` | 新增 user/model/provider-type 接口；统一前缀 `/api`、`/user` | ✅ |
| 1.4 | `src/contexts/AuthContext.tsx` | 仅当之前已登录时才弹窗（避免游客一打开就弹） | ✅ |
| 1.5 | `src/components/auth/LoginRegisterModal.tsx` | 登录/注册 tab + 60s 验证码倒计时 | ✅ |
| 1.6 | `src/components/auth/UserMenu.tsx` | 已登录显示头像下拉，未登录显示按钮 | ✅ |
| 1.7 | `src/App.tsx` | 不再屏蔽业务路由：始终渲染主布局；以 `user.userId` 作 key 重挂子树 | ✅ |
| 1.8 | `AgentChatView` 发送拦截 | 未登录时点发送 → 弹登录框，不发请求 | ✅ |
| 1.9 | `useAgents / useKnowledgeBases / ChatSessionsContext` | 未登录时不拉数据，登录后随 user 触发 | ✅ |

## Phase 2 — Agent 模型选择重做（任务 2）

| # | 文件 | 动作 | 状态 |
|---|---|---|---|
| 2.1 | `src/api/api.ts` | `CreateAgentRequest / UpdateAgentRequest / AgentVO` 字段 `model` → `modelId: number` + 展示 `modelName?: string`；新增 `ModelVO / getModels / createModel / getProviderTypes` | ✅ |
| 2.2 | `src/hooks/useModels.ts`（新建） | 列出 / 创建模型 | ✅ |
| 2.3 | `src/components/modals/AddAgentModal.tsx` | 模型下拉从 `useModels` 拉；下拉底部加固定的 `"+ 自定义模型"` 项，点击触发自定义弹窗 | ✅ |
| 2.4 | `src/components/modals/CustomModelModal.tsx`（新建） | `modelName / providerType（下拉，从后端拉） / baseUrl / apiKey`；点击蒙层或"取消"关闭；"确定" 调用 `POST /api/models` → 成功后自动选中该模型 | ✅ |
| 2.5 | `src/components/tabs/AgentTabContent.tsx` 等 | 所有引用 `agent.model` 的地方改成 `agent.modelName` | ✅ |

## Phase 3 — 侧边栏折叠（任务 3）

| # | 文件 | 动作 | 状态 |
|---|---|---|---|
| 3.1 | `src/contexts/SidebarContext.tsx` | `collapsed / toggle` | ✅ |
| 3.2 | `src/layout/Sidebar.tsx` | 宽度 320↔0，带 `transition-all duration-300` | ✅ |
| 3.3 | `SideMenu` 右上 `MenuFoldOutlined` 收起 + `OpenAgentLayout` 折叠时左上浮 `MenuUnfoldOutlined` 展开 | 双按钮方案 | ✅ |

## Phase 4 — 用户维度数据隔离（任务 4）

| # | 文件 | 动作 | 状态 |
|---|---|---|---|
| 4.1 | `useAgents / useKnowledgeBases / useModels` | 依赖 `user`；未登录清空 + 不发请求 | ✅ |
| 4.2 | `ChatSessionsContext` | 同上 | ✅ |
| 4.3 | `App.tsx` | `key={user?.userId ?? "guest"}` 切用户重挂子树 | ✅ |
| 4.4 | `AuthContext.logout` | 调后端 logout → 清 token → 清 user state | ✅ |

---

## 执行顺序

Phase 1 → Phase 2 → Phase 3 → Phase 4。每个 Phase 做完停下复核，再推进下一个。

## 风险与假设

- **Token 存储**：采用 `localStorage`，刷新页面后自动带 `authorization` header。后端 sa-token 配置 `auto-renew: true`，不用手动续签。
- **antd Select `dropdownRender`**：用于在下拉最末尾追加 "+ 自定义模型" 项，这是官方推荐做法。
- **未登录时的接口调用**：除 `login / register / sendRegisterCode` 外，其他调用若 401 统一清 token 跳登录，避免死循环。
