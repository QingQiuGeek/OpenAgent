import React, { useMemo } from "react";
import { Button, Dropdown, Modal, Tag } from "antd";
import type { MenuProps } from "antd";
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  MoreOutlined,
} from "@ant-design/icons";
import type { AgentVO } from "../../api/api.ts";
import { formatFullDateTime, getAgentEmoji } from "../../utils";
import { useSelectedAgent } from "../../contexts/SelectedAgentContext.tsx";

interface AgentTabContentProps {
  agents: AgentVO[];
  onCreateAgentClick: () => void;
  onSelectAgent: (agentId: string) => void;
  onEditAgent?: (agent: AgentVO) => void;
  onDeleteAgent?: (agentId: string) => void;
}

const AgentTabContent: React.FC<AgentTabContentProps> = ({
  agents,
  onCreateAgentClick,
  onSelectAgent,
  onEditAgent,
  onDeleteAgent,
}) => {
  const { selectedAgentId } = useSelectedAgent();

  // 为每个 agent 生成 emoji
  const agentsWithEmoji = useMemo(() => {
    return agents.map((agent) => ({
      ...agent,
      emoji: getAgentEmoji(agent.id),
    }));
  }, [agents]);

  // 创建右键菜单
  const getContextMenuItems = (agent: AgentVO): MenuProps["items"] => {
    const items: MenuProps["items"] = [];

    if (onEditAgent) {
      items.push({
        key: "edit",
        label: "编辑",
        icon: <EditOutlined />,
        onClick: (e) => {
          e.domEvent.stopPropagation();
          onEditAgent(agent);
        },
      });
    }

    if (onDeleteAgent) {
      items.push({
        key: "delete",
        label: "删除",
        icon: <DeleteOutlined />,
        danger: true,
        onClick: (e) => {
          e.domEvent.stopPropagation();
          Modal.confirm({
            title: "确定要删除这个智能体吗？",
            content: "删除后将无法恢复",
            okText: "确定",
            cancelText: "取消",
            okType: "danger",
            onOk: () => {
              onDeleteAgent(agent.id);
            },
          });
        },
      });
    }

    return items;
  };

  return (
    <div className="flex flex-col h-full">
      <Button
        color="geekblue"
        variant="filled"
        icon={<PlusOutlined />}
        onClick={onCreateAgentClick}
        className="w-full"
      >
        智能体助手
      </Button>
      <div className="flex-1 overflow-y-auto bg-gray-50 dark:bg-zinc-900 rounded-lg p-1.5">
        {agents.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-400">
            <p className="text-sm">暂无智能体</p>
            <p className="text-xs mt-1">点击上方按钮添加</p>
          </div>
        ) : (
          <div className="space-y-1.5">
            {agentsWithEmoji.map((agent) => {
              const menuItems = getContextMenuItems(agent);
              const hasMenu = menuItems && menuItems.length > 0;
              const isSelected = selectedAgentId === agent.id;
              return (
                <div
                  key={agent.id}
                  onClick={() => onSelectAgent(agent.id)}
                  className={`w-full px-2.5 py-2 rounded-lg cursor-pointer transition-all hover:shadow-sm group relative border flex items-center gap-2.5 ${
                    isSelected
                      ? "bg-green-50 dark:bg-green-900/30 border-green-400 dark:border-green-700 hover:bg-green-100 dark:hover:bg-green-900/50"
                      : "bg-white dark:bg-zinc-800 border-transparent dark:border-zinc-700 hover:bg-gray-100 dark:hover:bg-zinc-700"
                  }`}
                >
                  <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-yellow-200 to-orange-200 flex items-center justify-center shrink-0 text-lg">
                    {agent.emoji}
                  </div>
                  <div className="flex-1 min-w-0 leading-tight">
                    <div className="flex items-center gap-1.5">
                      <span className="font-medium text-sm text-gray-900 dark:text-gray-100 truncate">
                        {agent.name}
                      </span>
                      {agent.modelName && (
                        <Tag
                          color="geekblue"
                          className="!m-0 !text-[10px] !px-1 !py-0 !leading-4"
                        >
                          {agent.modelName}
                        </Tag>
                      )}
                    </div>
                    {agent.description && (
                      <div className="text-xs text-gray-500 dark:text-gray-400 mt-0.5 truncate">
                        {agent.description}
                      </div>
                    )}
                    {agent.createdAt && (
                      <div className="text-[11px] text-gray-400 dark:text-gray-500 mt-0.5 truncate">
                        创建时间：{formatFullDateTime(agent.createdAt)}
                      </div>
                    )}
                  </div>
                  {hasMenu && (
                    <div
                      onClick={(e) => e.stopPropagation()}
                      onContextMenu={(e) => e.stopPropagation()}
                      className="self-center shrink-0 opacity-0 group-hover:opacity-100 transition-opacity"
                    >
                      <Dropdown
                        menu={{ items: menuItems }}
                        trigger={["contextMenu", "click"]}
                        placement="bottomLeft"
                      >
                        <Button
                          type="text"
                          shape="circle"
                          icon={<MoreOutlined />}
                          onClick={(e) => e.stopPropagation()}
                          className="!w-8 !h-8 flex items-center justify-center text-gray-500 hover:text-gray-800"
                        />
                      </Dropdown>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default AgentTabContent;
