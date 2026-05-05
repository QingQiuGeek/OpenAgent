CREATE DATABASE openagent;
\connect   openagent;
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE "user" (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role varchar(10) NOT NULL,                     -- 角色（admin/user）
    mail varchar(100) NOT NULL UNIQUE,          -- 邮箱
    phone varchar(100) UNIQUE,                     -- 电话（可选）
    password varchar(100) ,           -- 密码（实际项目中应加密存储）
    user_name varchar(20) ,
    status int NOT NULL default 0,               -- 账户状态（0-正常，1-禁用）
    created_at TIMESTAMP not null DEFAULT NOW(),
    updated_at TIMESTAMP not null DEFAULT NOW(),
    is_deleted int DEFAULT 0
);

CREATE TABLE enum_config (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  type_code  VARCHAR(50)  NOT NULL,             -- 枚举类别：model_provider_type / agent_status ...
  item_code  VARCHAR(100) NOT NULL,             -- 枚举项 code：openai / anthropic ...
  item_label VARCHAR(100) NOT NULL,             -- 展示名
  extra      JSONB,                             -- 扩展（图标、默认 base_url 等）
  sort       INT NOT NULL DEFAULT 0,
  status     INT NOT NULL DEFAULT 0,            -- 0 正常 / 1 禁用
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  is_deleted INT NOT NULL DEFAULT 0,
  UNIQUE (type_code, item_code)
);

CREATE TABLE model (
     id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
     user_id       BIGINT NOT NULL,             -- 所属用户
     model_name    VARCHAR(50)  NOT NULL,       -- 实际模型标识 (如 "gpt-3.5-turbo", "qwen-turbo")
     provider_type VARCHAR(20),                 -- 厂商类型 (如 "openai", "anthropic", "ollama", "custom")
     base_url      VARCHAR(255) NOT NULL,       -- API 地址
     api_key       VARCHAR(255) NOT NULL,       -- API Key (生产环境建议加密存储)
     max_tokens    INTEGER,                     -- 该模型的最大上下文窗口
     created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
     updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
     is_deleted    INT NOT NULL DEFAULT 0
);

CREATE TABLE agent (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       BIGINT NOT NULL,             -- 所属用户
    name          TEXT NOT NULL,               -- Agent 名称
    description   TEXT,                        -- 描述（用户可见）
    system_prompt TEXT,                        -- 系统指令
    model_id      BIGINT REFERENCES model(id) ON DELETE SET NULL,  -- 默认使用的模型
    allowed_tools JSONB,                       -- 允许使用的工具列表
    allowed_kbs   JSONB,                       -- 允许访问的知识库
    chat_options  JSONB,                       -- 其它配置项（温度、top_p、最大token）
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP DEFAULT NOW(),
    is_deleted    INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_agent_user_updated ON agent (user_id, updated_at DESC);

CREATE TABLE chat_session (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    BIGINT NOT NULL,                                  -- 所属用户
    agent_id   UUID REFERENCES agent(id) ON DELETE SET NULL,     -- 绑定的 Agent
    title      TEXT,                                             -- 自动生成的标题
    metadata   JSONB,                                            -- 扩展（例如输入语言、设备类型）
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    is_deleted INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_chat_session_user_updated ON chat_session (user_id, updated_at DESC);

CREATE TABLE chat_message (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES chat_session(id) ON DELETE CASCADE,
    role       TEXT NOT NULL,                      -- user / assistant / system / tool
    content    TEXT,                               -- 主体内容
    metadata   JSONB,                              -- 工具调用、RAG 片段、模型参数等
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    is_deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE knowledge_base (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     BIGINT NOT NULL,                                  -- 所属用户
    name        TEXT NOT NULL,
    description TEXT,
    metadata    JSONB,                                            -- 业务属性，如行业/标签
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    is_deleted  INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_kb_user_updated ON knowledge_base (user_id, updated_at DESC);

CREATE TABLE document (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    BIGINT NOT NULL,                                  -- 所属用户（冗余，便于查询过滤）
    kb_id      UUID NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    filename   TEXT NOT NULL,
    filetype   TEXT,                                             -- pdf / md / txt 等
    size       BIGINT,                                           -- 文件大小
    metadata   JSONB,                                            -- 页数、上传方式、解析参数等
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    is_deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE chunk_bge_m3 (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    kb_id UUID NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    doc_id UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,

    content TEXT NOT NULL,                  -- 切片后的文本内容
    metadata JSONB,                         -- 页码、段落号、chunk index 等

    embedding VECTOR(1024) NOT NULL,        -- bge_m3 模型是 1024 维的向量

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 给向量加索引
CREATE INDEX idx_chunk_embedding
ON chunk_bge_m3
USING ivfflat (embedding vector_l2_ops)
WITH (lists = 100);

-- -----------------------------------------------------------------------------
-- mcp_server：用户自助注册 MCP Server，被 agent.allowed_tools 中 mcp:{id} 引用
-- -----------------------------------------------------------------------------
CREATE TABLE mcp_server (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    transport  VARCHAR(20)  NOT NULL,           -- stdio / sse / http
    command    TEXT,
    url        TEXT,
    headers    JSONB,
    env        JSONB,
    enabled    INT          NOT NULL DEFAULT 1,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_deleted INT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_mcp_server_user ON mcp_server (user_id, updated_at DESC);

-- -----------------------------------------------------------------------------
-- agent_usage_log：调用日志（Step 9 仅建表，暂不接业务写入）
-- -----------------------------------------------------------------------------
CREATE TABLE agent_usage_log (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT     NOT NULL,
    agent_id          UUID,
    session_id        UUID,
    model_id          BIGINT,
    prompt_tokens     INT,
    completion_tokens INT,
    total_tokens      INT,
    latency_ms        INT,
    status            VARCHAR(20),                 -- success / error / timeout
    error_msg         TEXT,
    created_at        TIMESTAMP  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_agent_usage_user_time ON agent_usage_log (user_id, created_at DESC);
