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
 * <p>布局：inline + 固定预留高度 (h-7)，避免之前绝对定位导致的两个问题：
 * (1) 操作按钮浮在下条消息头顶造成视觉重叠；
 * (2) 0 高度容器使光标在「气泡」与「按钮」之间瞬时离开 group，
 * 导致 group-hover 立即失效、按钮还没点到就消失。</p>
 * <p>采用 opacity 过渡而非 display:hidden，鼠标在保留空间内移动也能维持显示。</p>
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
    <div
      className={`flex h-6 items-center -mt-0.5 opacity-0 group-hover:opacity-100 transition-opacity duration-150 ${
        align === "end" ? "justify-end" : "justify-start"
      }`}
    >
      <Actions items={items} variant="borderless" />
    </div>
  );
};

export default MessageActions;
