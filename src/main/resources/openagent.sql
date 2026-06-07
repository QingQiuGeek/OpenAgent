CREATE DATABASE openagent;
\connect   openagent;

-- 启动pgvector向量扩展
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
  type       VARCHAR(50)  NOT NULL,             -- 枚举类别：model_provider_type / tool_type / document_filetype / mcp_transport
  value      VARCHAR(100) NOT NULL,             -- 枚举值：openai / pdf / stdio ...
  status     INT NOT NULL DEFAULT 0,            -- 0 正常 / 1 禁用
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  is_deleted INT NOT NULL DEFAULT 0,
  UNIQUE (type, value)
);

-- 字典表种子（与代码中的常量保持一致；ON CONFLICT 保证幂等）
INSERT INTO enum_config(type, value) VALUES
    ('model_provider_type','openai'),
    ('model_provider_type','deepseek'),
    ('model_provider_type','qianwen'),
    ('model_provider_type','zhipu'),
    ('model_provider_type','ollama'),
    ('model_provider_type','minimax'),
    ('tool_type','FIXED'),
    ('tool_type','OPTIONAL'),
    ('document_filetype','pdf'),
    ('document_filetype','docx'),
    ('document_filetype','doc'),
    ('document_filetype','xlsx'),
    ('document_filetype','xls'),
    ('document_filetype','md'),
    ('document_filetype','txt'),
    ('document_filetype','json'),
    ('document_filetype','yaml'),
    ('document_filetype','yml'),
    ('document_filetype','sql'),
    ('document_filetype','java'),
    ('document_filetype','js'),
    ('document_filetype','jsx'),
    ('document_filetype','ts'),
    ('document_filetype','tsx'),
    ('document_filetype','py'),
    ('document_filetype','go'),
    ('document_filetype','rs'),
    ('document_filetype','cpp'),
    ('document_filetype','c'),
    ('document_filetype','h'),
    ('document_filetype','cs'),
    ('document_filetype','rb'),
    ('document_filetype','php'),
    ('upload_filetype','pdf'),
    ('upload_filetype','docx'),
    ('upload_filetype','doc'),
    ('upload_filetype','xlsx'),
    ('upload_filetype','xls'),
    ('upload_filetype','md'),
    ('upload_filetype','txt'),
    ('upload_filetype','json'),
    ('upload_filetype','yaml'),
    ('upload_filetype','yml'),
    ('upload_filetype','sql'),
    ('upload_filetype','java'),
    ('upload_filetype','js'),
    ('upload_filetype','jsx'),
    ('upload_filetype','ts'),
    ('upload_filetype','tsx'),
    ('upload_filetype','py'),
    ('upload_filetype','go'),
    ('upload_filetype','rs'),
    ('upload_filetype','cpp'),
    ('upload_filetype','c'),
    ('upload_filetype','h'),
    ('upload_filetype','cs'),
    ('upload_filetype','rb'),
    ('upload_filetype','php'),
    ('upload_filetype','png'),
    ('upload_filetype','jpg'),
    ('upload_filetype','jpeg'),
    ('upload_filetype','gif'),
    ('upload_filetype','webp'),
    ('mcp_transport','stdio'),
    ('mcp_transport','sse'),
    ('mcp_transport','http')
ON CONFLICT (type, value) DO NOTHING;

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

-- -----------------------------------------------------------------------------
-- chat_feedback：消息点赞/点踩
-- -----------------------------------------------------------------------------
CREATE TABLE chat_feedback (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    message_id  UUID   NOT NULL REFERENCES chat_message(id) ON DELETE CASCADE,
    rating      SMALLINT NOT NULL,                    -- 1 赞 / -1 踩
    reason_tags JSONB,                                -- ["不准确","格式差"]
    comment     TEXT,
    created_at  TIMESTAMP DEFAULT NOW(),
    UNIQUE (user_id, message_id)
);
CREATE INDEX idx_feedback_message ON chat_feedback (message_id);

-- -----------------------------------------------------------------------------
-- share_link：会话分享公开页
-- -----------------------------------------------------------------------------
CREATE TABLE share_link (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     BIGINT NOT NULL,
    session_id  UUID NOT NULL REFERENCES chat_session(id) ON DELETE CASCADE,
    slug        VARCHAR(20) NOT NULL,                    -- 公开访问短码
    snapshot    JSONB,                                    -- 创建瞬间的会话快照
    expire_at   TIMESTAMP,                                -- NULL 表示永不过期
    view_count  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP DEFAULT NOW(),
    is_deleted  INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_share_slug_alive ON share_link (slug) WHERE is_deleted = 0;
CREATE INDEX idx_share_user_created ON share_link (user_id, created_at DESC);

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
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      BIGINT NOT NULL,                                  -- 所属用户（冗余，便于查询过滤）
    kb_id        UUID NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    filename     TEXT NOT NULL,
    filetype     TEXT,                                             -- pdf / md / txt 等
    size         BIGINT,                                           -- 文件大小
    metadata     JSONB,                                            -- 页数、上传方式、解析参数等
    content_hash VARCHAR(64),                                      -- 文档内容 SHA-256 摘要，用于变更检测
    status       TEXT NOT NULL DEFAULT 'uploading',                 -- uploading / vectorizing / done / failed / skipped
    error_msg    TEXT,                                              -- 失败时记录原因
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW(),
    is_deleted   INT NOT NULL DEFAULT 0
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
    chat_mode         VARCHAR(20),                 -- normal / agent / web_search ...
    prompt_tokens     INT,
    completion_tokens INT,
    total_tokens      INT,
    latency_ms        INT,
    status            VARCHAR(20),                 -- success / error / timeout
    error_msg         TEXT,
    created_at        TIMESTAMP  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_agent_usage_user_time   ON agent_usage_log (user_id, created_at DESC);
CREATE INDEX idx_agent_usage_model_time  ON agent_usage_log (model_id, created_at DESC);
CREATE INDEX idx_agent_usage_status_time ON agent_usage_log (status,   created_at DESC);