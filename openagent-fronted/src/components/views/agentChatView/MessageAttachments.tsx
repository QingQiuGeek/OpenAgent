import React from "react";
import { Image } from "antd";
import { FileCard } from "@ant-design/x";
import type { AttachmentItem } from "../../../types";

interface MessageAttachmentsProps {
  attachments: AttachmentItem[];
}

const isImageAttachment = (a: AttachmentItem) => {
  if (a.contentType && a.contentType.startsWith("image/")) return true;
  // 兜底：根据文件名扩展名判断
  return /\.(png|jpe?g|gif|webp|bmp|svg)$/i.test(a.name);
};

/**
 * 消息附件渲染：竖向一列，按上传顺序展示。
 * - 图片：antd Image，点击弹出大图（原尺寸预览）
 * - 其他：antd-x FileCard（点击下载）
 */
const MessageAttachments: React.FC<MessageAttachmentsProps> = ({
  attachments,
}) => {
  if (!attachments || attachments.length === 0) return null;

  return (
    <div className="flex flex-col items-end gap-1.5">
      {attachments.map((att) => {
        if (isImageAttachment(att)) {
          return (
            <Image
              key={att.url}
              src={att.url}
              alt={att.name}
              width={160}
              style={{ borderRadius: 8 }}
              // antd Image 默认 preview 行为即原尺寸预览（可放大缩小）
              preview={{ src: att.url }}
            />
          );
        }
        return (
          <FileCard
            key={att.url}
            name={att.name}
            byte={att.size}
            onClick={() => window.open(att.url, "_blank", "noopener")}
            style={{ width: 240, cursor: "pointer" }}
          />
        );
      })}
    </div>
  );
};

export default MessageAttachments;
