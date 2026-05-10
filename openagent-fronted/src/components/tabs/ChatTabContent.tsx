import React, { useMemo, useState } from "react";
import { useNavigate, useLocation, matchPath } from "react-router-dom";
import { Button, Popconfirm, Tag, Tooltip } from "antd";
import {
  PlusOutlined,
  MessageOutlined,
  DeleteOutlined,
  ShareAltOutlined,
} from "@ant-design/icons";
import ShareLinkModal from "../modals/ShareLinkModal.tsx";
import { useChatSessions } from "../../hooks/useChatSessions.ts";
import { useAgents } from "../../hooks/useAgents.ts";
import { useAuth } from "../../contexts/AuthContext.tsx";
import { useSelectedAgent } from "../../contexts/SelectedAgentContext.tsx";
import { formatFullDateTime, getAgentEmoji } from "../../utils";
import type { AgentVO } from "../../api/api.ts";

const ChatTabContent: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const match = matchPath("/chat/:chatSessionId", location.pathname);
  const activeChatSessionId = match?.params.chatSessionId;
  const { chatSessions, loading, deleteChatSession } = useChatSessions();
  const { agents } = useAgents();
  const { requireAuth } = useAuth();
  const { selectedAgentId } = useSelectedAgent();

  // agentId -> 完整 agent 映射（给渲染卡片和会话标题用）
  const agentMap = useMemo(() => {
    const map = new Map<string, AgentVO>();
    agents.forEach((agent) => map.set(agent.id, agent));
    return map;
  }, [agents]);

  const selectedAgent: AgentVO | undefined = selectedAgentId
    ? agentMap.get(selectedAgentId)
    : undefined;

  // 当选中智能体时，只展示该智能体的会话
  const visibleSessions = useMemo(() => {
    if (!selectedAgentId) return chatSessions;
    return chatSessions.filter((s) => s.agentId === selectedAgentId);
  }, [chatSessions, selectedAgentId]);

  const handleCreateNewChat = () => {
    if (!requireAuth()) return;
    navigate("/chat");
  };

  const handleSelectChatSession = (chatSessionId: string) => {
    navigate(`/chat/${chatSessionId}`);
  };

  const handleDeleteChatSession = async (chatSessionId: string) => {
    await deleteChatSession(chatSessionId);
  };

  // 分享弹框当前选中的 sessionId
  const [shareSessionId, setShareSessionId] = useState<string | null>(null);

  // 格式化标题显示
  const getDisplayTitle = (session: { title?: string; agentId: string }) => {
    if (session.title) {
      return session.title;
    }
    const info = agentMap.get(session.agentId);
    return info ? `与 ${info.name} 的对话` : "新对话";
  };

  return (
    <div className="flex flex-col h-full">
      {selectedAgent && (
        <div className="mb-2 w-full px-2.5 py-2 rounded-lg border flex items-center gap-2.5 bg-green-50 dark:bg-green-900/30 border-green-400 dark:border-green-700">
          <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-yellow-200 to-orange-200 flex items-center justify-center shrink-0 text-lg">
            {getAgentEmoji(selectedAgent.id)}
          </div>
          <div className="flex-1 min-w-0 leading-tight">
            <div className="flex items-center gap-1.5">
              <span className="font-medium text-sm text-gray-900 dark:text-gray-100 truncate">
                {selectedAgent.name}
              </span>
              {selectedAgent.modelName && (
                <Tag
                  color="geekblue"
                  className="!m-0 !text-[10px] !px-1 !py-0 !leading-4"
                >
                  {selectedAgent.modelName}
                </Tag>
              )}
            </div>
            {selectedAgent.description && (
              <div className="text-xs text-gray-500 dark:text-gray-400 mt-0.5 truncate">
                {selectedAgent.description}
              </div>
            )}
            {selectedAgent.createdAt && (
              <div className="text-[11px] text-gray-400 dark:text-gray-500 mt-0.5 truncate">
                创建时间：{formatFullDateTime(selectedAgent.createdAt)}
              </div>
            )}
          </div>
        </div>
      )}
      <Button
        color="geekblue"
        variant="filled"
        icon={<PlusOutlined />}
        onClick={handleCreateNewChat}
        className="w-full"
      >
        新聊天
      </Button>
      <div className="flex-1 min-h-0 overflow-y-auto bg-gray-50 dark:bg-zinc-900 rounded-lg">
        {loading ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-400">
            <p className="text-sm">加载中...</p>
          </div>
        ) : visibleSessions.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-400">
            <MessageOutlined className="text-4xl mb-2" />
            <p className="text-sm">
              {selectedAgentId ? "当前智能体暂无聊天记录" : "暂无聊天记录"}
            </p>
            <p className="text-xs mt-1">点击上方按钮创建新聊天</p>
          </div>
        ) : (
          <div className="space-y-1.5 p-1.5">
            {visibleSessions.map((session) => {
              // 后端 createChatSession 返回的 id 不带 dash，list 接口返回的 id 带 dash，统一去 dash 比较
              const norm = (id?: string) => id?.replace(/-/g, "");
              const isActive = norm(session.id) === norm(activeChatSessionId);
              return (
              <div
                key={session.id}
                onClick={() => handleSelectChatSession(session.id)}
                className={`w-full px-3 py-2 rounded-lg border cursor-pointer transition-all group relative ${
                  isActive
                    ? "bg-green-50 dark:bg-green-900/30 border-green-400 dark:border-green-700"
                    : "bg-white dark:bg-zinc-800 border-transparent hover:bg-gray-100 dark:hover:bg-zinc-700 hover:shadow-sm"
                }`}
              >
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-200 to-purple-200 flex items-center justify-center shrink-0 text-base">
                    <MessageOutlined />
                  </div>
                  <div className="flex-1 min-w-0 leading-tight">
                    <div className="font-medium text-sm text-gray-900 dark:text-gray-100 truncate">
                      {getDisplayTitle(session)}
                    </div>
                    {session.createdAt && (
                      <div className="text-[11px] text-gray-400 dark:text-gray-500 mt-0.5 truncate">
                        创建时间：{formatFullDateTime(session.createdAt)}
                      </div>
                    )}
                  </div>
                  <div
                    className="shrink-0 self-center flex items-center gap-1"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <Tooltip title="分享会话">
                      <Button
                        type="text"
                        size="small"
                        icon={<ShareAltOutlined />}
                        className="opacity-0 group-hover:opacity-100 transition-opacity"
                        onClick={(e) => {
                          e.stopPropagation();
                          setShareSessionId(session.id);
                        }}
                      />
                    </Tooltip>
                    <Popconfirm
                      title="确定要删除这条聊天记录吗？"
                      description="删除后将无法恢复"
                      onConfirm={() => handleDeleteChatSession(session.id)}
                      okText="确定"
                      cancelText="取消"
                    >
                      <Button
                        type="text"
                        size="small"
                        icon={<DeleteOutlined />}
                        className="opacity-0 group-hover:opacity-100 transition-opacity"
                        onClick={(e) => e.stopPropagation()}
                        danger
                      />
                    </Popconfirm>
                  </div>
                </div>
              </div>
              );
            })}
          </div>
        )}
      </div>
      <ShareLinkModal
        open={!!shareSessionId}
        sessionId={shareSessionId}
        onClose={() => setShareSessionId(null)}
      />
    </div>
  );
};

export default ChatTabContent;
