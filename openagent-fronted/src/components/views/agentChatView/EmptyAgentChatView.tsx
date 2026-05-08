import React, { useEffect, useState } from "react";
// useState 仍被 useLoopTypewriter 内部使用
import { Card, Typography } from "antd";
import AgentChatInput, { type ChatInputSubmitPayload } from "./AgentChatInput";
import type { AgentVO } from "../../../api/api.ts";

const { Title, Text } = Typography;

const WELCOME_TEXT = "Welcome to OpenAgent";

/** 循环打字机：逐字出现 -> 停顿 -> 逐字退退 -> 重复 */
function useLoopTypewriter(
  text: string,
  { typeSpeed = 110, pauseEnd = 1400, deleteSpeed = 50, pauseStart = 600 } = {},
) {
  const [shown, setShown] = useState("");
  useEffect(() => {
    let i = 0;
    let phase: "typing" | "holding" | "deleting" | "resting" = "typing";
    let timer: ReturnType<typeof setTimeout>;
    const tick = () => {
      if (phase === "typing") {
        i += 1;
        setShown(text.slice(0, i));
        if (i >= text.length) {
          phase = "holding";
          timer = setTimeout(tick, pauseEnd);
          return;
        }
        timer = setTimeout(tick, typeSpeed);
      } else if (phase === "holding") {
        phase = "deleting";
        timer = setTimeout(tick, deleteSpeed);
      } else if (phase === "deleting") {
        i -= 1;
        setShown(text.slice(0, i));
        if (i <= 0) {
          phase = "resting";
          timer = setTimeout(tick, pauseStart);
          return;
        }
        timer = setTimeout(tick, deleteSpeed);
      } else {
        phase = "typing";
        timer = setTimeout(tick, typeSpeed);
      }
    };
    timer = setTimeout(tick, typeSpeed);
    return () => clearTimeout(timer);
  }, [text, typeSpeed, pauseEnd, deleteSpeed, pauseStart]);
  return shown;
}

const QUICK_CARDS = [
  {
    emoji: "🤖",
    title: "智能对话",
    desc: "与 AI 助手进行智能对话",
  },
  {
    emoji: "💡",
    title: "知识问答",
    desc: "基于知识库进行准确问答",
  },
  {
    emoji: "💬",
    title: "快速开始",
    desc: "输入消息，立即开始对话",
  },
  {
    emoji: "⚡",
    title: "工具调用",
    desc: "绑定 MCP / 内置工具增强能力",
  },
];

interface DefaultAgentChatViewProps {
  /**
   * 接收完整 payload（与 {@link AgentChatView#handleSendMessage} 一致）。
   * 不能只转发 text，否则附件会被丢弃，导致新会话首条消息丢文件。
   */
  handleSendMessage: (payload: ChatInputSubmitPayload) => void;
  loading: boolean;
  agents: AgentVO[];
}

/**
 * 尚未创建会话时的空态。输入框提交后委托给父组件的 handleSendMessage，
 * 由父组件统一做：登录态校验、未选中智能体提示、创建会话并跳转。
 */
const EmptyAgentChatView: React.FC<DefaultAgentChatViewProps> = ({
  loading,
  handleSendMessage,
}) => {
  const typed = useLoopTypewriter(WELCOME_TEXT);

  return (
    <div className="flex flex-col h-full">
      <div className="flex-1 min-h-0 overflow-y-auto flex items-center justify-center p-6">
        <div className="max-w-3xl w-full">
          <div className="text-center mb-8">
            <h1
              className="mb-2 text-3xl md:text-4xl font-extrabold tracking-tight"
              style={{ minHeight: "1.5em" }}
            >
              {/* emoji 使用系统彩色 emoji 字体，不受主标题 cursive 影响 */}
              <span
                style={{
                  fontFamily:
                    "'Apple Color Emoji','Segoe UI Emoji','Noto Color Emoji',sans-serif",
                }}
                className="mr-2 align-middle"
              >
                😀
              </span>
              <span
                className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 dark:from-indigo-300 dark:via-purple-300 dark:to-pink-300"
                style={{
                  fontFamily:
                    "'Pacifico','Segoe Script','Comic Sans MS',cursive,system-ui",
                }}
              >
                {typed}
              </span>
              <span className="inline-block w-[2px] h-[0.9em] align-[-0.1em] ml-0.5 bg-indigo-500 animate-pulse" />
            </h1>
            <Text type="secondary" className="text-base">
              选择一个智能体助手开始聊天，或直接发送消息创建新会话
            </Text>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {QUICK_CARDS.map((c) => (
              <Card
                key={c.title}
                hoverable
                size="small"
                className="!rounded-xl cursor-pointer transition-all hover:shadow-md"
                styles={{ body: { padding: 12 } }}
              >
                <div className="flex items-center gap-3">
                  <div
                    className={`w-10 h-10 rounded-full bg-gradient-to-br ${c.gradient} flex items-center justify-center shrink-0 text-xl`}
                  >
                    <span
                      style={{
                        fontFamily:
                          "'Apple Color Emoji','Segoe UI Emoji','Noto Color Emoji',sans-serif",
                      }}
                    >
                      {c.emoji}
                    </span>
                  </div>
                  <div className="min-w-0">
                    <Title level={5} className="!mb-0.5 !text-sm truncate">
                      {c.title}
                    </Title>
                    <Text type="secondary" className="!text-xs line-clamp-1">
                      {c.desc}
                    </Text>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        </div>
      </div>
      <div className="shrink-0 bg-white dark:bg-zinc-800">
        <div className="px-4 pb-4 pt-4">
          <AgentChatInput
            isAgentRunning={loading}
            onSend={handleSendMessage}
          />
        </div>
      </div>
    </div>
  );
};

export default EmptyAgentChatView;
