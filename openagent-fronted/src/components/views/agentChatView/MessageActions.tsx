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
  /** 操作行水平对齐：'start' 左对齐（AI），'end' 右对齐（用户） */
  align?: "start" | "end";
}

/**
 * 消息操作行：基于 antd-x {@link Actions}，label 自动作为 tooltip。
 * 绝对定位 + 父级 h-0：完全脱离布局流，hover 显示时不会撑大气泡宽度，
 * 也不会把后续消息往下推。
 */
const MessageActions: React.FC<MessageActionsProps> = ({
  message,
  onDelete,
  onRetry,
  align = "start",
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

  const showRetry =
    message.role === "assistant" && typeof onRetry === "function";

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
    <div className="relative h-0 w-full pointer-events-none">
      <div
        className={`hidden group-hover:block absolute top-0.5 whitespace-nowrap pointer-events-auto ${
          align === "end" ? "right-0" : "left-0"
        }`}
      >
        <Actions items={items} variant="borderless" />
      </div>
    </div>
  );
};

export default MessageActions;
