import React, { useMemo } from "react";
import { Button, Dropdown, Modal } from "antd";
import type { MenuProps } from "antd";
import {
  BookOutlined,
  DeleteOutlined,
  EditOutlined,
  MoreOutlined,
  PlusOutlined,
} from "@ant-design/icons";
import type { KnowledgeBase } from "../../types";
import { formatFullDateTime, getKnowledgeBaseEmoji } from "../../utils";

interface KnowledgeBaseTabContentProps {
  knowledgeBases: KnowledgeBase[];
  onCreateKnowledgeBaseClick?: () => void;
  onSelectKnowledgeBase?: (knowledgeBaseId: string) => void;
  onEditKnowledgeBase?: (kb: KnowledgeBase) => void;
  onDeleteKnowledgeBase?: (knowledgeBaseId: string) => void;
}

// 描述截取长度，避免过长溢出
const DESCRIPTION_PREVIEW_LIMIT = 40;

function truncate(text: string, max: number): string {
  if (!text) return "";
  return text.length > max ? `${text.slice(0, max)}…` : text;
}

const KnowledgeBaseTabContent: React.FC<KnowledgeBaseTabContentProps> = ({
  knowledgeBases,
  onCreateKnowledgeBaseClick,
  onSelectKnowledgeBase,
  onEditKnowledgeBase,
  onDeleteKnowledgeBase,
}) => {
  // 为每个知识库生成 emoji
  const knowledgeBasesWithEmoji = useMemo(() => {
    return knowledgeBases.map((kb) => ({
      ...kb,
      emoji: getKnowledgeBaseEmoji(kb.knowledgeBaseId),
    }));
  }, [knowledgeBases]);

  const getContextMenuItems = (kb: KnowledgeBase): MenuProps["items"] => {
    const items: MenuProps["items"] = [];

    if (onEditKnowledgeBase) {
      items.push({
        key: "edit",
        label: "编辑",
        icon: <EditOutlined />,
        onClick: (e) => {
          e.domEvent.stopPropagation();
          onEditKnowledgeBase(kb);
        },
      });
    }

    if (onDeleteKnowledgeBase) {
      items.push({
        key: "delete",
        label: "删除",
        icon: <DeleteOutlined />,
        danger: true,
        onClick: (e) => {
          e.domEvent.stopPropagation();
          Modal.confirm({
            title: "确定要删除这个知识库吗？",
            content: "删除后将无法恢复，知识库下的文档也会一同失效",
            okText: "确定",
            cancelText: "取消",
            okType: "danger",
            onOk: () => {
              onDeleteKnowledgeBase(kb.knowledgeBaseId);
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
        onClick={onCreateKnowledgeBaseClick}
        className="w-full"
      >
        新建知识库
      </Button>
      <div className="flex-1 overflow-y-auto bg-gray-50 dark:bg-zinc-900 rounded-lg p-1.5">
        {knowledgeBases.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-400">
            <BookOutlined className="text-4xl mb-2" />
            <p className="text-sm">暂无知识库</p>
            <p className="text-xs mt-1">点击上方按钮创建</p>
          </div>
        ) : (
          <div className="space-y-1.5">
            {knowledgeBasesWithEmoji.map((kb) => {
              const menuItems = getContextMenuItems(kb);
              const hasMenu = menuItems && menuItems.length > 0;
              return (
                <div
                  key={kb.knowledgeBaseId}
                  onClick={() => onSelectKnowledgeBase?.(kb.knowledgeBaseId)}
                  className="w-full px-2.5 py-2 rounded-lg cursor-pointer transition-all hover:shadow-sm group relative border border-transparent dark:border-zinc-700 bg-white dark:bg-zinc-800 hover:bg-gray-100 dark:hover:bg-zinc-700 flex items-center gap-2.5"
                >
                  <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-blue-200 to-purple-200 flex items-center justify-center shrink-0 text-lg">
                    {kb.emoji}
                  </div>
                  <div className="flex-1 min-w-0 leading-tight">
                    <div className="font-medium text-sm text-gray-900 dark:text-gray-100 truncate">
                      {kb.name}
                    </div>
                    {kb.description && (
                      <div className="text-xs text-gray-500 dark:text-gray-400 mt-0.5 truncate">
                        {truncate(kb.description, DESCRIPTION_PREVIEW_LIMIT)}
                      </div>
                    )}
                    {kb.createdAt && (
                      <div className="text-[11px] text-gray-400 dark:text-gray-500 mt-0.5 truncate">
                        创建时间：{formatFullDateTime(kb.createdAt)}
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

export default KnowledgeBaseTabContent;
