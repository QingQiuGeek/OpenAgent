export type MessageType = "user" | "assistant" | "system" | "tool";

export interface KnowledgeBase {
  knowledgeBaseId: string;
  name: string;
  description: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ToolCall {
  id: string;
  type: string;
  name: string;
  arguments: string;
}

export interface ToolResponse {
  id: string;
  name: string;
  responseData: string;
}

/** 来源引用条目：网络搜索 / 知识库召回 */
export interface SourceItem {
  /** "web" 网络搜索；"kb" 知识库 */
  type: "web" | "kb";
  /** 网络搜索：网页标题；知识库：被检索到的文件名/分块标题 */
  title: string;
  /** 网络搜索：URL；知识库：可能为空 */
  url?: string;
  /** 网络搜索：内容摘要；知识库：分块文本 */
  content?: string;
  /** 网络搜索：相关度得分 */
  score?: number;
  /** 知识库名称（kb 类型时使用） */
  kbName?: string;
}

/** 用户消息附件：上传到 OSS 后的元数据，跟随 message.metadata 一起入库 */
export interface AttachmentItem {
  /** OSS 公网访问 URL，可直接下载 / 预览 */
  url: string;
  /** 原始文件名 */
  name: string;
  /** 字节数 */
  size?: number;
  /** MIME 类型，如 image/png */
  contentType?: string;
}

export interface ChatMessageVOMetadata {
  toolCalls?: ToolCall[];
  toolResponse?: ToolResponse;
  /** 来源引用，assistant 终态消息会带 */
  sources?: SourceItem[];
  /** 用户消息上传的附件 */
  attachments?: AttachmentItem[];
}

export interface ChatMessageVO {
  id: string;
  sessionId: string;
  role: MessageType;
  content: string;
  metadata?: ChatMessageVOMetadata;
}

export type SseMessageType =
  | "AI_GENERATED_CONTENT"
  | "AI_MESSAGE_CHUNK"
  | "AI_PLANNING"
  | "AI_THINKING"
  | "AI_EXECUTING"
  | "AI_DONE"
  | "AI_ERROR";

export interface SseMessagePayload {
  message: ChatMessageVO;
  statusText: string;
  done: boolean;
  delta?: string;
}

export interface SseMessageMetadata {
  chatMessageId: string;
}

export interface SseMessage {
  type: SseMessageType;
  payload: SseMessagePayload;
  metadata: SseMessageMetadata;
}
