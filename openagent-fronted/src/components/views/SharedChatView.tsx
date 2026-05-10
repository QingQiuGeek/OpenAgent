import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Avatar, Empty, Result, Spin, Tag, Typography } from "antd";
import { Bubble } from "@ant-design/x";
import XMarkdown from "@ant-design/x-markdown";
import { RobotOutlined, ToolOutlined } from "@ant-design/icons";
import {
  viewSharePublic,
  type ShareSnapshotMessage,
  type ShareSnapshotVO,
} from "../../api/share";

/** 共享快照里 metadata 的最小子集（与后端 ChatMessageDTO.MetaData 对齐） */
interface SnapshotToolCall {
  id?: string;
  name?: string;
  arguments?: string;
}
interface SnapshotToolResponse {
  id?: string;
  name?: string;
  responseData?: string;
}
interface SnapshotMetadata {
  toolCalls?: SnapshotToolCall[];
  toolResponse?: SnapshotToolResponse;
}

const AVATAR_SIZE = 36;

const USER_AVATAR = (
  <Avatar size={AVATAR_SIZE} style={{ backgroundColor: "transparent", fontSize: 24 }}>
    😃
  </Avatar>
);

const AI_AVATAR = <Avatar src="/logo.jpg" size={AVATAR_SIZE} />;

const AI_BUBBLE_STYLES = {
  content: {
    background: "transparent",
    padding: 0,
    minWidth: 0,
    maxWidth: "100%",
    boxShadow: "none",
    border: "none",
  } as React.CSSProperties,
};

const SharedChatView: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const [data, setData] = useState<ShareSnapshotVO | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    if (!slug) return;
    let cancelled = false;
    setLoading(true);
    viewSharePublic(slug)
      .then((vo) => {
        if (!cancelled) setData(vo);
      })
      .catch((err: Error) => {
        if (!cancelled) setErrorMsg(err.message || "分享不存在或已过期");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [slug]);

  if (loading) {
    return (
      <div className="h-screen flex items-center justify-center">
        <Spin />
      </div>
    );
  }

  if (errorMsg || !data) {
    return (
      <div className="h-screen flex items-center justify-center">
        <Result status="404" title="无法访问该分享" subTitle={errorMsg ?? "分享不存在或已过期"} />
      </div>
    );
  }

  const messages = (data.messages ?? []) as ShareSnapshotMessage[];

  return (
    <div className="h-screen overflow-y-auto bg-white dark:bg-zinc-900">
      <div className="mx-auto max-w-3xl px-6 py-8">
        <header className="mb-6 border-b border-gray-200 dark:border-zinc-700 pb-4">
          <Typography.Title level={3} className="!mb-1">
            {data.title || "分享会话"}
          </Typography.Title>
          <div className="text-sm text-gray-500">
            {data.agentName ? `由「${data.agentName}」生成` : "由 OpenAgent 生成"}
            {data.createdAt && (
              <>
                <span className="mx-1">·</span>
                <span>创建于 {new Date(data.createdAt).toLocaleString()}</span>
              </>
            )}
            {typeof data.viewCount === "number" && (
              <>
                <span className="mx-1">·</span>
                <span>浏览 {data.viewCount} 次</span>
              </>
            )}
          </div>
        </header>

        {messages.length === 0 ? (
          <Empty description="该会话还没有消息" />
        ) : (
          <div className="space-y-4">
            {messages.map((m) => (
              <SharedMessage key={m.id} message={m} />
            ))}
          </div>
        )}

        <footer className="mt-10 text-center text-xs text-gray-400">
          这是只读分享页，不会同步原会话后续消息。
        </footer>
      </div>
    </div>
  );
};

/** 工具调用卡片：展示工具名 + 参数（折叠 JSON） */
const ToolCallCard: React.FC<{ call: SnapshotToolCall }> = ({ call }) => {
  let pretty = call.arguments ?? "";
  try {
    if (pretty) pretty = JSON.stringify(JSON.parse(pretty), null, 2);
  } catch {
    /* 保留原始字符串 */
  }
  return (
    <div className="border border-gray-200 dark:border-zinc-700 rounded-md bg-gray-50 dark:bg-zinc-800/60 p-2 max-w-full overflow-hidden">
      <div className="flex items-center gap-1.5 text-xs text-gray-700 dark:text-gray-300">
        <ToolOutlined />
        <span>调用工具</span>
        <Tag color="blue" className="!m-0">
          {call.name || "unknown"}
        </Tag>
      </div>
      {pretty && (
        <pre className="mt-1 mb-0 text-[12px] leading-snug text-gray-600 dark:text-gray-400 whitespace-pre-wrap break-all max-h-40 overflow-auto">
          {pretty}
        </pre>
      )}
    </div>
  );
};

/** 工具返回卡片：展示工具名 + 返回摘要 */
const ToolResponseCard: React.FC<{ resp: SnapshotToolResponse }> = ({ resp }) => {
  const text = resp.responseData ?? "";
  return (
    <div className="border border-gray-200 dark:border-zinc-700 rounded-md bg-emerald-50/60 dark:bg-emerald-900/10 p-2 max-w-full overflow-hidden">
      <div className="flex items-center gap-1.5 text-xs text-gray-700 dark:text-gray-300">
        <ToolOutlined />
        <span>工具返回</span>
        <Tag color="green" className="!m-0">
          {resp.name || "unknown"}
        </Tag>
      </div>
      {text && (
        <div className="mt-1 text-[12px] leading-snug text-gray-700 dark:text-gray-300 whitespace-pre-wrap break-all max-h-40 overflow-auto">
          {text}
        </div>
      )}
    </div>
  );
};

const SharedMessage: React.FC<{ message: ShareSnapshotMessage }> = ({ message }) => {
  const meta = (message.metadata ?? {}) as SnapshotMetadata;

  if (message.role === "system") {
    return (
      <div className="flex justify-center">
        <div className="px-3 py-1 bg-gray-100 dark:bg-zinc-800 text-gray-600 dark:text-gray-300 text-xs rounded-full flex items-center gap-1">
          <RobotOutlined />
          <span>{message.content}</span>
        </div>
      </div>
    );
  }
  if (message.role === "tool") {
    // 工具角色独立 bubble：直接渲染工具返回卡片（公开页面也保留，便于审阅）
    if (!meta.toolResponse) return null;
    return (
      <Bubble
        avatar={AI_AVATAR}
        placement="start"
        style={{ width: "100%", maxWidth: "100%" }}
        styles={AI_BUBBLE_STYLES}
        content={<ToolResponseCard resp={meta.toolResponse} />}
      />
    );
  }
  if (message.role === "user") {
    return (
      <Bubble
        avatar={USER_AVATAR}
        placement="end"
        content={
          <div className="text-[15px] leading-relaxed whitespace-pre-wrap break-words">
            {message.content}
          </div>
        }
      />
    );
  }
  // assistant：先渲染本轮触发的 toolCalls 卡片，再渲染正文 Markdown
  const toolCalls = meta.toolCalls ?? [];
  const hasContent = !!(message.content && message.content.trim());
  return (
    <Bubble
      avatar={AI_AVATAR}
      placement="start"
      style={{ width: "100%", maxWidth: "100%" }}
      styles={AI_BUBBLE_STYLES}
      content={
        <div className="w-full min-w-0 max-w-full space-y-2">
          {toolCalls.length > 0 && (
            <div className="flex flex-col gap-2">
              {toolCalls.map((c, i) => (
                <ToolCallCard key={c.id ?? i} call={c} />
              ))}
            </div>
          )}
          {hasContent && (
            <div className="markdown-body min-w-0">
              <XMarkdown>{message.content || ""}</XMarkdown>
            </div>
          )}
        </div>
      }
    />
  );
};

export default SharedChatView;
