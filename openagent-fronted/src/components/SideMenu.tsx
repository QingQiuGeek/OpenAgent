import React, { useState } from "react";
import { MenuFoldOutlined } from "@ant-design/icons";
import { Button, Image, Tabs, Tooltip, type TabsProps } from "antd";
import { useNavigate } from "react-router-dom";
import AgentTabContent from "./tabs/AgentTabContent.tsx";
import AddAgentModal from "./modals/AddAgentModal.tsx";
import ChatTabContent from "./tabs/ChatTabContent.tsx";
import KnowledgeBaseTabContent from "./tabs/KnowledgeBaseTabContent.tsx";
import AddKnowledgeBaseModal from "./modals/AddKnowledgeBaseModal.tsx";
import { useAgents } from "../hooks/useAgents.ts";
import { useKnowledgeBases } from "../hooks/useKnowledgeBases.ts";
import { useSidebar } from "../contexts/SidebarContext.tsx";
import { useAuth } from "../contexts/AuthContext.tsx";
import { useSelectedAgent } from "../contexts/SelectedAgentContext.tsx";

interface SideMenuProps {
  children?: React.ReactNode;
}

const SideMenu: React.FC<SideMenuProps> = () => {
  const navigate = useNavigate();
  const { toggle: toggleSidebar } = useSidebar();
  const { requireAuth } = useAuth();
  const { selectAgent } = useSelectedAgent();

  const [isAddAgentModalOpen, setIsAddAgentModalOpen] = useState(false);
  const toggleAddAgentModal = () => {
    // 关闭已打开的弹窗不需要登录拦截
    if (!isAddAgentModalOpen && !requireAuth()) return;
    setIsAddAgentModalOpen(!isAddAgentModalOpen);
    setEditingAgent(null);
  };

  const [editingAgent, setEditingAgent] = useState<
    import("../api/api.ts").AgentVO | null
  >(null);

  /**
   * 添加/编辑知识库模态框状态
   */
  const [isAddKnowledgeBaseModalOpen, setIsAddKnowledgeBaseModalOpen] =
    useState(false);
  const [editingKnowledgeBase, setEditingKnowledgeBase] = useState<
    import("../types").KnowledgeBase | null
  >(null);
  const toggleAddKnowledgeBaseModal = () => {
    if (!isAddKnowledgeBaseModalOpen && !requireAuth()) return;
    setIsAddKnowledgeBaseModalOpen(!isAddKnowledgeBaseModalOpen);
    // 关闭时清理编辑状态
    if (isAddKnowledgeBaseModalOpen) {
      setEditingKnowledgeBase(null);
    }
  };
  const { agents, createAgentHandle, deleteAgentHandle, updateAgentHandle } =
    useAgents();

  const [activeKey, setActiveKey] = useState(() => {
    if (location.pathname.startsWith("/agent")) return "agent";
    if (location.pathname.startsWith("/knowledge-base")) return "knowledgeBase";
    if (location.pathname.startsWith("/chat")) return "chat";
    return "agent";
  });

  const {
    knowledgeBases,
    createKnowledgeBaseHandle,
    updateKnowledgeBaseHandle,
    deleteKnowledgeBaseHandle,
  } = useKnowledgeBases();

  // 处理标签页切换
  const handleTabChange = (key: string) => {
    setActiveKey(key);
  };

  const items: TabsProps["items"] = [
    {
      key: "agent",
      label: <span className="select-none">智能体助手</span>,
      children: (
        <AgentTabContent
          agents={agents}
          onSelectAgent={(agentId) => {
            selectAgent(agentId);
            navigate("/chat");
            setActiveKey("chat");
          }}
          onCreateAgentClick={toggleAddAgentModal}
          onEditAgent={(agent) => {
            setEditingAgent(agent);
            setIsAddAgentModalOpen(true);
          }}
          onDeleteAgent={deleteAgentHandle}
        />
      ),
    },
    {
      key: "chat",
      label: <span className="select-none">聊天记录</span>,
      children: <ChatTabContent />,
    },
    {
      key: "knowledgeBase",
      label: <span className="select-none">知识库</span>,
      children: (
        <KnowledgeBaseTabContent
          knowledgeBases={knowledgeBases}
          onCreateKnowledgeBaseClick={toggleAddKnowledgeBaseModal}
          onSelectKnowledgeBase={(knowledgeBaseId) => {
            navigate(`/knowledge-base/${knowledgeBaseId}`);
          }}
          onEditKnowledgeBase={(kb) => {
            setEditingKnowledgeBase(kb);
            setIsAddKnowledgeBaseModalOpen(true);
          }}
          onDeleteKnowledgeBase={deleteKnowledgeBaseHandle}
        />
      ),
    },
  ];

  return (
    <div className="px-4 flex flex-col h-full">
      <div className="h-14 w-full flex items-center justify-between border-b border-gray-200 dark:border-zinc-700">
        <div className="flex items-center gap-2 mx-2">
         
          <img
            src="/logo.jpg"
            sizes="large"
            alt="OpenAgent"
            className="w-15 h-15 rounded object-cover"
          />
          <div className="text-lg font-semibold select-none text-gray-900 dark:text-gray-100">
            OpenAgent
          </div>
        </div>
          <Button
            type="text"
            icon={<MenuFoldOutlined />}
            onClick={toggleSidebar}
          />
      </div>
      <div className="flex-1 min-h-0 flex flex-col">
        <Tabs
          activeKey={activeKey}
          onChange={handleTabChange}
          items={items}
        />
      </div>
      <AddAgentModal
        open={isAddAgentModalOpen}
        onClose={toggleAddAgentModal}
        createAgentHandle={createAgentHandle}
        updateAgentHandle={updateAgentHandle}
        editingAgent={editingAgent}
      />
      <AddKnowledgeBaseModal
        open={isAddKnowledgeBaseModalOpen}
        onClose={toggleAddKnowledgeBaseModal}
        createKnowledgeBaseHandle={createKnowledgeBaseHandle}
        updateKnowledgeBaseHandle={updateKnowledgeBaseHandle}
        editingKnowledgeBase={editingKnowledgeBase}
      />
    </div>
  );
};

export default SideMenu;
