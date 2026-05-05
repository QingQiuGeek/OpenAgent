import React from "react";
import { Actions } from "@ant-design/x";
import { message as antdMessage } from "antd";
import {
  CopyOutlined,
  DeleteOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import type { ChatMessageVO } from "../../../types";

interface MessageActionsProps {
  message: ChatMessageVO;
  onDelete?: (id: string) => void;
  onRetry?: (message: ChatMessageVO) => void;
}

const MessageActions: React.FC<MessageActionsProps> = ({
  message,
  onDelete,
  onRetry,
}) => {
  const handleCopy = () => {
    const text = message.content || "";
    if (!text) {
      antdMessage.info("内容为空，无需复制");
      return;
    }
    navigator.clipboard
      .writeText(text)
      .then(() => antdMessage.success("已复制到剪贴板"))
      .catch(() => antdMessage.error("复制失败"));
  };

  // 只有 assistant 消息可以 retry（重新生成）
  const showRetry = message.role === "assistant" && typeof onRetry === "function";

  const items = [
    {
      key: "copy",
      label: "复制",
      icon: <CopyOutlined />,
      onItemClick: handleCopy,
    },
    ...(showRetry
      ? [
          {
            key: "retry",
            label: "重试",
            icon: <ReloadOutlined />,
            onItemClick: () => onRetry?.(message),
          },
        ]
      : []),
    {
      key: "delete",
      label: "删除",
      icon: <DeleteOutlined />,
      danger: true,
      onItemClick: () => onDelete?.(message.id),
    },
  ];

  return (
    <div className="opacity-0 group-hover:opacity-100 transition-opacity">
      <Actions items={items} variant="borderless" />
    </div>
  );
};

export default MessageActions;
