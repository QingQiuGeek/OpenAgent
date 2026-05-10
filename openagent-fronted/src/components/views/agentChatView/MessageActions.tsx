import React, { useEffect, useState } from "react";
import { Actions } from "@ant-design/x";
import { Button, Popover, Checkbox, Input, message as antdMessage } from "antd";
import {
  CopyOutlined,
  DeleteOutlined,
  ReloadOutlined,
  LikeOutlined,
  LikeFilled,
  DislikeOutlined,
  DislikeFilled,
} from "@ant-design/icons";
import type { ChatMessageVO } from "../../../types";
import {
  getFeedback,
  submitFeedback,
  withdrawFeedback,
} from "../../../api/chatFeedback";

const REASON_TAGS = ["不准确", "不相关", "格式差", "太冗长", "太简短", "其他"];

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
  const showFeedback = message.role === "assistant";

  // 反馈状态：0=未评价 / 1=赞 / -1=踩
  const [rating, setRating] = useState<0 | 1 | -1>(0);
  const [reasonTags, setReasonTags] = useState<string[]>([]);
  const [comment, setComment] = useState("");
  const [reasonOpen, setReasonOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // 加载初始反馈
  useEffect(() => {
    if (!showFeedback) return;
    let cancelled = false;
    getFeedback(message.id)
      .then((vo) => {
        if (cancelled || !vo) return;
        setRating((vo.rating === 1 || vo.rating === -1 ? vo.rating : 0) as 0 | 1 | -1);
        setReasonTags(vo.reasonTags ?? []);
        setComment(vo.comment ?? "");
      })
      .catch(() => {
        // 忽略：没反馈也不是错误
      });
    return () => {
      cancelled = true;
    };
  }, [message.id, showFeedback]);

  const submit = async (next: 1 | -1, tags?: string[], cmt?: string) => {
    if (submitting) return;
    setSubmitting(true);
    try {
      await submitFeedback({
        messageId: message.id,
        rating: next,
        reasonTags: tags,
        comment: cmt,
      });
      setRating(next);
      antdMessage.success("反馈已提交");
    } catch {
      // http 层已 toast
    } finally {
      setSubmitting(false);
    }
  };

  const withdraw = async () => {
    if (submitting) return;
    setSubmitting(true);
    try {
      await withdrawFeedback(message.id);
      setRating(0);
      setReasonTags([]);
      setComment("");
    } finally {
      setSubmitting(false);
    }
  };

  const handleLike = () => {
    if (rating === 1) {
      void withdraw();
    } else {
      void submit(1);
    }
  };

  const handleDislikeOpen = (open: boolean) => {
    setReasonOpen(open);
    if (open && rating !== -1) {
      // 首次点踩：先写入 -1，再补充原因
      void submit(-1);
    }
  };

  const reasonContent = (
    <div className="w-64">
      <div className="text-xs text-gray-500 mb-2">为什么踩？（可选）</div>
      <Checkbox.Group
        options={REASON_TAGS}
        value={reasonTags}
        onChange={(vals) => setReasonTags(vals as string[])}
        className="!flex !flex-wrap !gap-y-1"
      />
      <Input.TextArea
        rows={2}
        placeholder="补充说明（可选）"
        value={comment}
        onChange={(e) => setComment(e.target.value)}
        className="!mt-2"
        maxLength={200}
      />
      <div className="flex justify-end gap-2 mt-2">
        <Button size="small" onClick={() => { setReasonOpen(false); }}>
          取消
        </Button>
        <Button size="small" danger onClick={async () => { await withdraw(); setReasonOpen(false); }}>
          撤销踩
        </Button>
        <Button
          size="small"
          type="primary"
          loading={submitting}
          onClick={async () => {
            await submit(-1, reasonTags, comment);
            setReasonOpen(false);
          }}
        >
          提交
        </Button>
      </div>
    </div>
  );

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
      className={`flex h-6 items-center gap-1 -mt-0.5 opacity-0 group-hover:opacity-100 transition-opacity duration-150 ${
        align === "end" ? "justify-end" : "justify-start"
      }`}
    >
      <Actions items={items} variant="borderless" />
      {showFeedback && (
        <>
          <Button
            size="small"
            type="text"
            icon={rating === 1 ? <LikeFilled className="!text-blue-500" /> : <LikeOutlined />}
            onClick={handleLike}
            disabled={submitting}
          />
          <Popover
            placement="top"
            trigger="click"
            open={reasonOpen}
            onOpenChange={handleDislikeOpen}
            content={reasonContent}
            destroyOnHidden
          >
            <Button
              size="small"
              type="text"
              icon={rating === -1 ? <DislikeFilled className="!text-red-500" /> : <DislikeOutlined />}
              disabled={submitting}
            />
          </Popover>
        </>
      )}
    </div>
  );
};

export default MessageActions;
