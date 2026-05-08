import React, { useRef, useState } from "react";
import { Attachments, Sender } from "@ant-design/x";
import { Button, message as antdMessage } from "antd";
import {
  PaperClipOutlined,
  BulbOutlined,
  GlobalOutlined,
} from "@ant-design/icons";
import type { GetProp, GetRef } from "antd";
import { uploadFile, type UploadFileResponse } from "../../../api/api";

export interface ChatInputSubmitPayload {
  text: string;
  deepThink: boolean;
  webSearch: boolean;
  /** 已经上传成功的附件元数据（粘贴/选择时即上传，发送时只携带元数据） */
  uploadedAttachments: UploadFileResponse[];
}

interface AgentChatInputProps {
  onSend: (payload: ChatInputSubmitPayload) => void;
  isAgentRunning?: boolean;
  onStop?: () => void;
}

// 支持的文件扩展名（含图片）
const ACCEPT_EXTS = [
  ".xlsx",
  ".xls",
  ".doc",
  ".docx",
  ".md",
  ".txt",
  ".pdf",
  ".java",
  ".js",
  ".jsx",
  ".ts",
  ".tsx",
  ".py",
  ".go",
  ".rs",
  ".cpp",
  ".c",
  ".h",
  ".cs",
  ".rb",
  ".php",
  ".sql",
  ".json",
  ".yaml",
  ".yml",
  ".png",
  ".jpg",
  ".jpeg",
  ".gif",
  ".webp",
];

const ACCEPT_STR = ACCEPT_EXTS.join(",");
/** 单文件大小上限（MB），与后端 FileController.MAX_FILE_SIZE_BYTES 对齐 */
const MAX_FILE_SIZE_MB = 5;
/** 单条消息附件数量上限 */
const MAX_FILE_COUNT = 5;
/** 提示文案：鼠标悬停回形针按钮时展示 */
const UPLOAD_HINT = `最多 ${MAX_FILE_COUNT} 个文件，单个不超过 ${MAX_FILE_SIZE_MB}MB；仅支持文档/代码/图片，不支持视频、音频。`;

// AttachmentItem 在 antd-x 内部基于 UploadFile，自带 status / percent。
// 这里再扩展一个 uploadedMeta 字段：上传成功后保存后端返回的 OSS 元数据。
type AttachmentItem = GetProp<typeof Attachments, "items">[number] & {
  uploadedMeta?: UploadFileResponse;
};

const AgentChatInput: React.FC<AgentChatInputProps> = ({
  onSend,
  isAgentRunning = false,
  onStop,
}) => {
  const [message, setMessage] = useState("");
  const [deepThink, setDeepThink] = useState(false);
  const [webSearch, setWebSearch] = useState(false);
  const [items, setItems] = useState<AttachmentItem[]>([]);
  const attachmentsRef = useRef<GetRef<typeof Attachments>>(null);

  const handleSubmit = () => {
    if (isAgentRunning) return;
    const trimmed = message.trim();
    if (!trimmed) return;
    // 任一附件还在上传 → 阻止发送，避免文件信息丢失
    const stillUploading = items.some((it) => it.status === "uploading");
    if (stillUploading) {
      antdMessage.warning("文件还在上传中，请稍候");
      return;
    }
    // 上传失败的附件直接忽略，不会进消息
    const uploadedAttachments = items
      .map((it) => it.uploadedMeta)
      .filter((m): m is UploadFileResponse => !!m);
    onSend({
      text: trimmed,
      deepThink,
      webSearch,
      uploadedAttachments,
    });
    setMessage("");
    setItems([]);
  };

  const senderHeader = items.length > 0 && (
    <Sender.Header
      title="附件"
      open={items.length > 0}
      styles={{ content: { padding: 0 } }}
    >
      <Attachments
        ref={attachmentsRef}
        beforeUpload={() => false}
        items={items}
        onChange={({ fileList }) => setItems(fileList as AttachmentItem[])}
        accept={ACCEPT_STR}
      />
    </Sender.Header>
  );

  // 根据 uid 找到 item 并合并字段（避免 stale state）
  const patchItem = (uid: string, patch: Partial<AttachmentItem>) => {
    setItems((prev) =>
      prev.map((it) =>
        it.uid === uid ? ({ ...it, ...patch } as AttachmentItem) : it,
      ),
    );
  };

  // 把原生 File[] 追加进附件列表（按扩展名 + 大小过滤），并立即上传到后端 OSS。
  // - 图片预先生成 blob URL 作为 thumbUrl，避免 antd-x 内部低分辨率 canvas 缩略图
  // - status="uploading" 让 Attachments 显示转圈进度，上传完成切到 "done"
  const appendFiles = (files: File[]) => {
    if (!files.length) return;
    // 按剩余名额接收；超出部分一次性提醒后丢弃
    const remaining = MAX_FILE_COUNT - items.length;
    if (remaining <= 0) {
      antdMessage.warning(`最多只能上传 ${MAX_FILE_COUNT} 个文件`);
      return;
    }
    if (files.length > remaining) {
      antdMessage.warning(
        `本次只能再上传 ${remaining} 个文件，其余已忽略`,
      );
    }
    const candidates = files.slice(0, remaining);
    const accepted: AttachmentItem[] = [];
    for (const f of candidates) {
      const lowerName = f.name.toLowerCase();
      const extOk = ACCEPT_EXTS.some((ext) => lowerName.endsWith(ext));
      if (!extOk) {
        antdMessage.warning(`${f.name} 不支持的文件类型，已跳过`);
        continue;
      }
      // 全面拦截视频 / 音频 MIME（防止用户伪造扩展名）
      if (f.type && (f.type.startsWith("video/") || f.type.startsWith("audio/"))) {
        antdMessage.warning(`${f.name} 不支持视频 / 音频文件，已跳过`);
        continue;
      }
      if (f.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
        antdMessage.warning(`${f.name} 超过 ${MAX_FILE_SIZE_MB}MB，已跳过`);
        continue;
      }
      const isImage =
        (f.type && f.type.startsWith("image/")) ||
        /\.(png|jpe?g|gif|webp|bmp|svg)$/i.test(f.name);
      const blobUrl = isImage ? URL.createObjectURL(f) : undefined;
      const uid = `${Date.now()}-${f.name}-${Math.random().toString(36).slice(2, 8)}`;
      accepted.push({
        uid,
        name: f.name,
        size: f.size,
        status: "uploading",
        percent: 0,
        originFileObj: f,
        ...(blobUrl ? { thumbUrl: blobUrl, url: blobUrl } : {}),
      } as unknown as AttachmentItem);
    }
    if (accepted.length === 0) return;
    setItems((prev) => [...prev, ...accepted]);

    // 并发触发上传；每个文件独立结算 status / uploadedMeta，互不阻塞
    accepted.forEach((item) => {
      const file = item.originFileObj as File;
      uploadFile(file)
        .then((meta) => {
          patchItem(item.uid as string, {
            status: "done",
            percent: 100,
            uploadedMeta: meta,
          } as Partial<AttachmentItem>);
        })
        .catch((err) => {
          console.error("[AgentChatInput] uploadFile error", err);
          antdMessage.error(
            err instanceof Error
              ? `${item.name} 上传失败：${err.message}`
              : `${item.name} 上传失败`,
          );
          patchItem(item.uid as string, {
            status: "error",
            percent: 0,
          } as Partial<AttachmentItem>);
        });
    });
  };

  const handleAddFiles = () => {
    // 通过隐藏 input 触发文件选择
    const input = document.createElement("input");
    input.type = "file";
    input.multiple = true;
    input.accept = ACCEPT_STR;
    input.onchange = () => {
      if (!input.files || input.files.length === 0) return;
      appendFiles(Array.from(input.files));
    };
    input.click();
  };

  // Sender 提供 onPasteFile：粘贴板里有文件时直接加入附件
  const handlePasteFile = (files: FileList) => {
    appendFiles(Array.from(files));
  };

  return (
    <Sender
      loading={isAgentRunning}
      onSubmit={handleSubmit}
      onCancel={() => onStop?.()}
      placeholder={isAgentRunning ? "智能体回复中..." : "输入消息..."}
      value={message}
      onChange={setMessage}
      header={senderHeader}
      onPasteFile={handlePasteFile}
      classNames={{ suffix: "!hidden" }}
      footer={(_oriNode, { components }) => {
        const { SendButton, LoadingButton } = components;
        return (
          <div className="flex items-center justify-between w-full pt-1">
            <div className="flex items-center gap-1">
              <Button
                type="text"
                size="small"
                icon={<PaperClipOutlined />}
                onClick={handleAddFiles}
                title={UPLOAD_HINT}
                disabled={items.length >= MAX_FILE_COUNT}
              />
              <Button
                type={deepThink ? "primary" : "text"}
                size="small"
                icon={<BulbOutlined />}
                onClick={() => setDeepThink((v) => !v)}
                className={deepThink ? "" : "!text-gray-600"}
              >
                深度思考
              </Button>
              <Button
                type={webSearch ? "primary" : "text"}
                size="small"
                icon={<GlobalOutlined />}
                onClick={() => setWebSearch((v) => !v)}
                className={webSearch ? "" : "!text-gray-600"}
              >
                联网搜索
              </Button>
            </div>
            <div>
              {isAgentRunning ? <LoadingButton type="default" /> : <SendButton type="primary" />}
            </div>
          </div>
        );
      }}
    />
  );
};

export default AgentChatInput;
