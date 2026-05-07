import React, { useState, useRef, useEffect, useCallback } from "react";
import { Avatar } from "antd";
import { Bubble } from "@ant-design/x";
import XMarkdown from "@ant-design/x-markdown";
import {
  ToolOutlined,
  CheckCircleOutlined,
  RobotOutlined,
  DownOutlined,
  RightOutlined,
  LoadingOutlined,
} from "@ant-design/icons";
import type { ChatMessageVO, SseMessageType, ToolCall, ToolResponse } from "../../../types";
import PreBlock from "./CodeBlock";
import MessageActions from "./MessageActions";

interface AgentChatHistoryProps {
  messages: ChatMessageVO[];
  streamingContent?: string;
  isAgentRunning?: boolean;
  displayAgentStatus?: boolean;
  agentStatusText?: string;
  agentStatusType?: SseMessageType;
  onDeleteMessage?: (id: string) => void;
  onRetryMessage?: (message: ChatMessageVO) => void;
}

// 头像尺寸常量（用户与 AI 保持一致）
const AVATAR_SIZE = 40;

// 用户头像（emoji，无背景色）
const USER_AVATAR = (
  <Avatar
    size={AVATAR_SIZE}
    style={{ backgroundColor: "transparent", fontSize: 28, lineHeight: 1 }}
  >
    😃
  </Avatar>
);

// XMarkdown 自定义组件映射：用 PreBlock 替换 <pre>，实现高亮 + 复制 + mermaid
const MARKDOWN_COMPONENTS = { pre: PreBlock };

// AI 气泡样式：去掉默认背景与内边距，直接显示页面底色
const AI_BUBBLE_STYLES = {
  content: {
    background: "transparent",
    padding: 0,
    boxShadow: "none",
    border: "none",
  } as React.CSSProperties,
};

// 工具调用展示组件（简化版，用于 assistant 消息内）
const ToolCallDisplay: React.FC<{ toolCall: ToolCall }> = ({ toolCall }) => {
  let parsedArgs: Record<string, unknown> = {};
  try {
    parsedArgs = JSON.parse(toolCall.arguments) as Record<string, unknown>;
  } catch {
    // 如果解析失败，使用原始字符串
  }

  const argCount = Object.keys(parsedArgs).length;
  const argPreview = argCount > 0 
    ? Object.keys(parsedArgs).slice(0, 2).join(", ") + (argCount > 2 ? "..." : "")
    : toolCall.arguments.slice(0, 50) + (toolCall.arguments.length > 50 ? "..." : "");

  return (
    <div className="text-xs text-gray-500 flex items-center gap-1.5">
      <ToolOutlined className="text-blue-500" />
      <span className="font-mono text-blue-600">{toolCall.name}</span>
      {argPreview && (
        <>
          <span className="text-gray-400">·</span>
          <span className="text-gray-500 truncate max-w-[200px]">{argPreview}</span>
        </>
      )}
    </div>
  );
};

// 工具响应展示组件（可折叠）
const ToolResponseDisplay: React.FC<{ toolResponse: ToolResponse }> = ({
  toolResponse,
}) => {
  const [expanded, setExpanded] = useState(false);
  
  let parsedData: unknown = null;
  let isJson = false;
  let dataPreview = "";
  
  try {
    parsedData = JSON.parse(toolResponse.responseData);
    isJson = true;
    const jsonStr = JSON.stringify(parsedData);
    dataPreview = jsonStr.length > 100 ? jsonStr.slice(0, 100) + "..." : jsonStr;
  } catch {
    dataPreview = toolResponse.responseData.length > 100 
      ? toolResponse.responseData.slice(0, 100) + "..." 
      : toolResponse.responseData;
  }

  return (
    <div className="my-1.5 text-xs">
      <div 
        className="flex items-center gap-2 text-gray-500 cursor-pointer hover:text-gray-700 transition-colors"
        onClick={() => setExpanded(!expanded)}
      >
        {expanded ? (
          <DownOutlined className="text-gray-400" />
        ) : (
          <RightOutlined className="text-gray-400" />
        )}
        <CheckCircleOutlined className="text-green-500" />
        <span className="font-mono text-green-600">{toolResponse.name}</span>
        <span className="text-gray-400">·</span>
        <span className="text-gray-500 truncate flex-1">{dataPreview}</span>
      </div>
      {expanded && (
        <div className="ml-5 mt-1.5 p-2 bg-gray-50 rounded border border-gray-200">
          <div className="text-xs text-gray-600 font-mono">
            {isJson ? (
              <pre className="whitespace-pre-wrap break-words overflow-x-auto max-h-60 overflow-y-auto">
                {JSON.stringify(parsedData, null, 2)}
              </pre>
            ) : (
              <div className="whitespace-pre-wrap break-words">
                {toolResponse.responseData}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

const AgentChatHistory: React.FC<AgentChatHistoryProps> = ({
  messages,
  streamingContent = "",
  isAgentRunning = false,
  displayAgentStatus = false,
  agentStatusText = "",
  agentStatusType,
  onDeleteMessage,
  onRetryMessage,
}) => {
  // 滚动容器引用
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  // 是否允许自动滚动（用户是否接近底部）
  const [isNearBottom, setIsNearBottom] = useState(true);
  // 容错阈值（像素）
  const SCROLL_THRESHOLD = 20;
  // 上一次消息数量，用于检测新消息
  const prevMessagesLengthRef = useRef(messages.length);

  // 检查是否接近底部
  const checkIfNearBottom = useCallback(() => {
    const container = scrollContainerRef.current;
    if (!container) return false;

    const { scrollTop, clientHeight, scrollHeight } = container;
    const distanceFromBottom = scrollHeight - scrollTop - clientHeight;
    return distanceFromBottom <= SCROLL_THRESHOLD;
  }, []);

  // 滚动到底部
  const scrollToBottom = useCallback(() => {
    const container = scrollContainerRef.current;
    if (!container) return;

    // 使用 requestAnimationFrame 确保 DOM 更新完成后再滚动
    requestAnimationFrame(() => {
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    });
  }, []);

  // 处理滚动事件，实时更新是否接近底部的状态
  const handleScroll = useCallback(() => {
    const nearBottom = checkIfNearBottom();
    setIsNearBottom(nearBottom);
  }, [checkIfNearBottom]);

  // 监听滚动事件
  useEffect(() => {
    const container = scrollContainerRef.current;
    if (!container) return;

    // 初始化时检查是否在底部（延迟执行以避免同步 setState）
    const initTimer = setTimeout(() => {
      setIsNearBottom(checkIfNearBottom());
    }, 0);

    container.addEventListener("scroll", handleScroll, { passive: true });

    return () => {
      clearTimeout(initTimer);
      container.removeEventListener("scroll", handleScroll);
    };
  }, [handleScroll, checkIfNearBottom]);

  // 监听消息变化，决定是否自动滚动
  useEffect(() => {
    const hasNewMessage = messages.length > prevMessagesLengthRef.current;
    prevMessagesLengthRef.current = messages.length;

    if (!hasNewMessage) return;

    // 用户自己刚发出的新消息 → 强制滚到底部，无视当前滚动位置
    const lastMessage = messages[messages.length - 1];
    if (lastMessage && lastMessage.role === "user") {
      scrollToBottom();
      setIsNearBottom(true);
      return;
    }

    // 其它新消息（AI 生成）：仅当用户已经在底部附近时才跟随
    if (isNearBottom) {
      scrollToBottom();
    }
  }, [messages, isNearBottom, scrollToBottom]);

  // 当 displayAgentStatus 变化时，如果用户接近底部，也自动滚动
  useEffect(() => {
    if (displayAgentStatus && isNearBottom) {
      scrollToBottom();
    }
  }, [displayAgentStatus, isNearBottom, scrollToBottom]);

  // 获取状态标签
  const getStatusLabel = () => {
    switch (agentStatusType) {
      case "AI_PLANNING":
        return "规划中";
      case "AI_THINKING":
        return "思考中";
      case "AI_EXECUTING":
        return "执行中";
      default:
        return "处理中";
    }
  };

  return (
    <div
      ref={scrollContainerRef}
      className="flex-1 min-w-0 overflow-y-auto overflow-x-hidden scrollbar-thick"
    >
      {/*
        聊天区域宽度由下面的 max-w-4xl 控制（约 896px）。
        如需调整，修改此处 class，例如 max-w-3xl(768)、max-w-5xl(1024)、max-w-[960px] 等。
      */}
      <div className="mx-auto max-w-4xl px-6 pt-4">
      {messages.map((message) => {
        return (
          <div className="mb-4 group" key={message.id}>
            {/* Assistant 消息 */}
            {message.role === "assistant" && (
              <Bubble
                styles={AI_BUBBLE_STYLES}
                avatar={<Avatar src="/logo.jpg" size={AVATAR_SIZE} />}
                content={
                  <div className="w-full min-w-0 max-w-full overflow-x-auto">
                    {/* 工具调用展示 */}
                    {message.metadata?.toolCalls &&
                      message.metadata.toolCalls.length > 0 && (
                        <div className="mb-2 flex flex-wrap gap-2">
                          {message.metadata.toolCalls.map((toolCall) => (
                            <ToolCallDisplay key={toolCall.id} toolCall={toolCall} />
                          ))}
                        </div>
                      )}
                    {/* 消息内容 */}
                    {message.content && (
                      <div className="markdown-body min-w-0">
                        <XMarkdown
                          components={MARKDOWN_COMPONENTS}
                          streaming={{ enableAnimation: false, hasNextChunk: true }}
                        >
                          {message.content}
                        </XMarkdown>
                      </div>
                    )}
                  </div>
                }
                placement="start"
                footer={
                  message.content ? (
                    <MessageActions
                      message={message}
                      onDelete={onDeleteMessage}
                      onRetry={onRetryMessage}
                    />
                  ) : null
                }
                footerPlacement="outer-start"
              />
            )}

            {/* Tool 消息 - 简洁展示，不使用气泡 */}
            {message.role === "tool" && message.metadata?.toolResponse && (
              <div className="flex justify-start">
                <div className="max-w-[85%]">
                  <ToolResponseDisplay toolResponse={message.metadata.toolResponse} />
                </div>
              </div>
            )}

            {/* User 消息 */}
            {message.role === "user" && (
              <Bubble
                avatar={USER_AVATAR}
                content={
                  <div className="text-[15px] leading-relaxed whitespace-pre-wrap break-words">
                    {message.content}
                  </div>
                }
                placement="end"
                footer={
                  <MessageActions
                    message={message}
                    onDelete={onDeleteMessage}
                  />
                }
                footerPlacement="outer-start"
              />
            )}

            {/* System 消息 */}
            {message.role === "system" && (
              <div className="flex justify-center">
                <div className="px-3 py-1 bg-gray-100 text-gray-600 text-xs rounded-full flex items-center gap-1">
                  <RobotOutlined />
                  <span>{message.content}</span>
                </div>
              </div>
            )}
          </div>
        );
      })}
      {isAgentRunning && !streamingContent && (
        <div className="mb-4">
          <Bubble
            styles={AI_BUBBLE_STYLES}
            avatar={<Avatar src="/logo.jpg" size={AVATAR_SIZE} />}
            content={
              <div className="flex items-center gap-2 text-gray-500 dark:text-gray-400">
                <LoadingOutlined spin />
                <span>思考中...</span>
              </div>
            }
            placement="start"
          />
        </div>
      )}
      {streamingContent && (
        <div className="mb-4">
          <Bubble
            styles={AI_BUBBLE_STYLES}
            avatar={<Avatar src="/logo.jpg" size={AVATAR_SIZE} />}
            content={
              <div className="w-full min-w-0 max-w-full overflow-x-auto">
                <div className="markdown-body min-w-0">
                  <XMarkdown
                    components={MARKDOWN_COMPONENTS}
                    streaming={{ enableAnimation: false, hasNextChunk: true }}
                  >
                    {streamingContent}
                  </XMarkdown>
                </div>
              </div>
            }
            placement="start"
          />
        </div>
      )}
      {displayAgentStatus && (
        <div className="mb-3">
          <div
            className="animate-pulse"
            style={{
              animation: "pulse 0.8s cubic-bezier(0.4, 0, 0.6, 1) infinite",
              filter: "brightness(1.15)",
            }}
          >
            <Bubble
              content={
                <span className="flex items-center gap-2">
                  <span
                    className="font-semibold text-blue-600"
                    style={{
                      animation:
                        "pulse 0.7s cubic-bezier(0.4, 0, 0.6, 1) infinite",
                      textShadow:
                        "0 0 10px rgba(37, 99, 235, 1), 0 0 20px rgba(37, 99, 235, 0.8), 0 0 30px rgba(37, 99, 235, 0.5)",
                      filter: "brightness(1.3)",
                    }}
                  >
                    ✨ {getStatusLabel()}
                  </span>
                  <span className="text-gray-400">·</span>
                  <span className="text-gray-600">{agentStatusText}</span>
                </span>
              }
              placement="start"
            />
          </div>
        </div>
      )}
      </div>
    </div>
  );
};

export default AgentChatHistory;
