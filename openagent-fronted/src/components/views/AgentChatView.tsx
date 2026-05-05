import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import { message as antdMessage } from "antd";
import AgentChatHistory from "./agentChatView/AgentChatHistory.tsx";
import AgentChatInput from "./agentChatView/AgentChatInput.tsx";
import ChatHeader from "./agentChatView/ChatHeader.tsx";
import {
  createChatMessage,
  createChatSession,
  deleteChatMessage,
  getChatMessagesBySessionId,
  getChatSession,
  stopAgent,
} from "../../api/api.ts";
import { useAgents } from "../../hooks/useAgents.ts";
import { useChatSessions } from "../../hooks/useChatSessions.ts";
import { useAuth } from "../../contexts/AuthContext.tsx";
import { useSelectedAgent } from "../../contexts/SelectedAgentContext.tsx";
import EmptyAgentChatView from "./agentChatView/EmptyAgentChatView.tsx";
import type { ChatMessageVO, SseMessage, SseMessageType } from "../../types";

const AgentChatView: React.FC = () => {
  const { chatSessionId } = useParams<{ chatSessionId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as { initMessage?: string } | null;
  const [loading, setLoading] = useState(false);
  const { agents } = useAgents();
  const { refreshChatSessions } = useChatSessions();
  const { user, openAuthModal } = useAuth();
  const { selectedAgentId, selectAgent } = useSelectedAgent();

  const [messages, setMessages] = useState<ChatMessageVO[]>([]);
  // 流式输出缓冲区：当前正在接收 chunk 的 AI 消息内容
  const [streamingContent, setStreamingContent] = useState<string>("");
  // Agent 是否正在运行（用户已发消息但 AI_DONE 未到达）
  const [isAgentRunning, setIsAgentRunning] = useState<boolean>(false);

  const addMessage = (message: ChatMessageVO) => {
    setMessages((prevMessages) => [...prevMessages, message]);
  };

  // 当前会话绑定的 agent（读取会话详情后填入）
  const [sessionAgentId, setSessionAgentId] = useState<string>("");

  // 以 sessionAgentId 为准，否则使用侧边栏选中的 agent
  const activeAgentId = sessionAgentId || selectedAgentId || "";

  const activeAgent = useMemo(
    () => agents.find((a) => a.id === activeAgentId),
    [agents, activeAgentId],
  );

  const getChatMessages = useCallback(async () => {
    if (!chatSessionId) return;
    const resp = await getChatMessagesBySessionId(chatSessionId);
    setMessages(resp.chatMessages);

    const session = await getChatSession(chatSessionId);
    setSessionAgentId(session.chatSession.agentId);
    // 进入一个会话时，把侧边栏选中同步到该会话的 agent
    if (session.chatSession.agentId) {
      selectAgent(session.chatSession.agentId);
    }
  }, [chatSessionId, selectAgent]);

  useEffect(() => {
    if (!chatSessionId) {
      setSessionAgentId("");
      setMessages([]);
      setStreamingContent("");
      setIsAgentRunning(false);
      return;
    }
    getChatMessages().then();
  }, [chatSessionId, getChatMessages]);

  // 新建会话后自动发送第一条用户消息
  useEffect(() => {
    const initMsg = state?.initMessage;
    if (!chatSessionId || !initMsg) return;
    // 清除 location state，防止重复发送
    navigate(location.pathname, { replace: true, state: {} });
    const agentId = selectedAgentId;
    if (!agentId) return;
    setIsAgentRunning(true);
    createChatMessage({
      agentId,
      sessionId: chatSessionId,
      role: "user",
      content: initMsg,
    })
      .then(() => getChatMessages())
      .catch((err) => {
        console.error(err);
        setIsAgentRunning(false);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chatSessionId]);

  const handleSendMessage = async (value: string | { text: string }) => {
    const text = typeof value === "string" ? value : value.text;
    if (!text || !text.trim()) return;

    if (!user) {
      openAuthModal();
      return;
    }

    if (!activeAgentId) {
      antdMessage.warning("请先选择智能体！");
      return;
    }

    if (!chatSessionId) {
      setLoading(true);
      try {
        const response = await createChatSession({
          agentId: activeAgentId,
          title: text.slice(0, 20),
        });
        await refreshChatSessions();
        navigate(`/chat/${response.chatSessionId}`, {
          replace: true,
          state: { initMessage: text },
        });
      } catch (error) {
        console.error("创建聊天会话失败:", error);
        antdMessage.error("创建聊天会话失败，请重试");
      } finally {
        setLoading(false);
      }
    } else {
      setIsAgentRunning(true);
      try {
        await createChatMessage({
          agentId: activeAgentId,
          sessionId: chatSessionId,
          role: "user",
          content: text,
        });
        await getChatMessages();
      } catch (err) {
        console.error(err);
        setIsAgentRunning(false);
      }
    }
  };

  // 终止 Agent 输出
  const handleStopAgent = async () => {
    if (!chatSessionId) return;
    try {
      await stopAgent(chatSessionId);
    } catch (err) {
      console.error("终止 Agent 失败:", err);
    }
    // 立即结束本地等待状态，AI_DONE 到达后会再次触发
    setIsAgentRunning(false);
  };

  // 删除消息
  const handleDeleteMessage = async (messageId: string) => {
    try {
      await deleteChatMessage(messageId);
      await getChatMessages();
      antdMessage.success("已删除");
    } catch (err) {
      console.error("删除消息失败:", err);
      antdMessage.error("删除失败");
    }
  };

  // 重试：重发最近一条 user 消息以重新生成 AI 回复
  const handleRetryMessage = async (message: ChatMessageVO) => {
    if (!chatSessionId || !activeAgentId) return;
    // 找到 message 之前最近的一条 user 消息
    const idx = messages.findIndex((m) => m.id === message.id);
    if (idx <= 0) return;
    let lastUserMsg: ChatMessageVO | undefined;
    for (let i = idx - 1; i >= 0; i--) {
      if (messages[i].role === "user") {
        lastUserMsg = messages[i];
        break;
      }
    }
    if (!lastUserMsg) {
      antdMessage.warning("找不到可重发的用户消息");
      return;
    }
    setIsAgentRunning(true);
    try {
      await createChatMessage({
        agentId: activeAgentId,
        sessionId: chatSessionId,
        role: "user",
        content: lastUserMsg.content,
      });
      await getChatMessages();
    } catch (err) {
      console.error("重试失败:", err);
      setIsAgentRunning(false);
    }
  };

  const [displayAgentStatus, setDisplayAgentStatus] = useState<boolean>(false);
  const [agentStatusText, setAgentStatusText] = useState("");
  const [agentStatusType, setAgentStatusType] = useState<
    SseMessageType | undefined
  >(undefined);

  useEffect(() => {
    if (!chatSessionId || !user) return;
    const es = new EventSource(
      `http://localhost:8080/sse/connect/${chatSessionId}`,
      { withCredentials: true },
    );
    es.onmessage = (event) => {
      console.log("Received message:", event.data);
    };
    es.onerror = (error) => {
      console.error("SSE error:", error);
    };

    es.addEventListener("message", (event) => {
      const msg = JSON.parse(event.data) as SseMessage;
      if (msg.type === "AI_MESSAGE_CHUNK") {
        const delta = msg.payload.delta ?? "";
        if (delta) {
          setStreamingContent((prev) => prev + delta);
        }
      } else if (msg.type === "AI_GENERATED_CONTENT") {
        // 完整消息已落库，清空流式缓冲，渲染最终消息
        setStreamingContent("");
        addMessage(msg.payload.message);
      } else if (msg.type === "AI_PLANNING") {
        setDisplayAgentStatus(true);
        setAgentStatusText(msg.payload.statusText);
        setAgentStatusType("AI_PLANNING");
      } else if (msg.type === "AI_THINKING") {
        setDisplayAgentStatus(true);
        setAgentStatusText(msg.payload.statusText);
        setAgentStatusType("AI_THINKING");
      } else if (msg.type === "AI_EXECUTING") {
        setDisplayAgentStatus(true);
        setAgentStatusText(msg.payload.statusText);
        setAgentStatusType("AI_EXECUTING");
      } else if (msg.type === "AI_DONE") {
        setDisplayAgentStatus(false);
        setAgentStatusText("");
        setAgentStatusType(undefined);
        setStreamingContent("");
        setIsAgentRunning(false);
      } else {
        throw new Error(`Unknown message type: ${msg.type}`);
      }
    });

    es.addEventListener("init", (event) => {
      console.log("Received init message:", event.data);
    });

    return () => {
      es.close();
    };
  }, [chatSessionId, user]);

  const headerTitle = activeAgent?.name ?? "智能体对话";
  const headerSubtitle = activeAgent?.modelName ?? undefined;
  const headerDescription = activeAgent?.description ?? undefined;

  if (!chatSessionId) {
    return (
      <div className="flex flex-col h-full">
        <ChatHeader title={headerTitle} subtitle={headerSubtitle} description={headerDescription} />
        <div className="flex-1 min-h-0">
          <EmptyAgentChatView
            agents={agents}
            loading={loading}
            handleSendMessage={handleSendMessage}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full min-w-0 overflow-hidden">
      <ChatHeader title={headerTitle} subtitle={headerSubtitle} description={headerDescription} />
      <AgentChatHistory
        messages={messages}
        streamingContent={streamingContent}
        isAgentRunning={isAgentRunning}
        displayAgentStatus={displayAgentStatus}
        agentStatusText={agentStatusText}
        agentStatusType={agentStatusType}
        onDeleteMessage={handleDeleteMessage}
        onRetryMessage={handleRetryMessage}
      />
      {/* 输入框宽度与聊天区域保持一致，由下面的 max-w-4xl 控制 */}
      <div className="px-6 py-4 bg-white dark:bg-zinc-800">
        <div className="mx-auto max-w-4xl">
          <AgentChatInput
            onSend={handleSendMessage}
            isAgentRunning={isAgentRunning}
            onStop={handleStopAgent}
          />
        </div>
      </div>
    </div>
  );
};

export default AgentChatView;
