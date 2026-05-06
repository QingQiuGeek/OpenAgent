import { get, post, patch, del, BASE_URL } from "./http.ts";
import type { ChatMessageVO, MessageType } from "../types";

// =============== 用户 ===============

export interface LoginUserVO {
  userId: number;
  userName?: string;
  mail?: string;
  role?: string;
}

export interface UserLoginDTO {
  mail: string;
  password: string;
}

export interface UserRegisterDTO {
  mail: string;
  password: string;
  rePassword: string;
  code: string;
}

export interface UserRegisterMailDTO {
  mail: string;
}

export async function login(body: UserLoginDTO): Promise<LoginUserVO> {
  return post<LoginUserVO>("/user/login", body, { skipAuth: true });
}

export async function register(body: UserRegisterDTO): Promise<LoginUserVO> {
  return post<LoginUserVO>("/user/register", body, { skipAuth: true });
}

export async function sendRegisterCode(body: UserRegisterMailDTO): Promise<boolean> {
  return post<boolean>("/user/register-code", body, { skipAuth: true });
}

export async function logout(): Promise<boolean> {
  return post<boolean>("/user/logout");
}

export async function getLoginUser(): Promise<LoginUserVO> {
  return get<LoginUserVO>("/user");
}

// =============== Provider Type ===============

export interface ProviderTypeVO {
  code: string;
  description: string;
}

export async function getProviderTypes(): Promise<ProviderTypeVO[]> {
  return get<ProviderTypeVO[]>("/api/provider-types");
}

// =============== Model ===============

export interface ModelVO {
  id: number;
  userId?: number;
  modelName: string;
  providerType: string;
  baseUrl?: string;
  apiKey?: string;
  maxTokens?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateModelRequest {
  modelName: string;
  providerType: string;
  baseUrl: string;
  apiKey: string;
  maxTokens?: number;
}

export interface UpdateModelRequest {
  modelName?: string;
  providerType?: string;
  baseUrl?: string;
  apiKey?: string;
  maxTokens?: number;
}

export interface CreateModelResponse {
  modelId: number;
}

export interface GetModelsResponse {
  models: ModelVO[];
}

export async function getModels(): Promise<GetModelsResponse> {
  return get<GetModelsResponse>("/api/models");
}

export async function createModel(
  body: CreateModelRequest,
): Promise<CreateModelResponse> {
  return post<CreateModelResponse>("/api/models", body);
}

export async function updateModel(
  modelId: number,
  body: UpdateModelRequest,
): Promise<boolean> {
  return patch<boolean>(`/api/models/${modelId}`, body);
}

export async function deleteModel(modelId: number): Promise<boolean> {
  return del<boolean>(`/api/models/${modelId}`);
}

// =============== Agent ===============

export interface ChatOptions {
  temperature?: number;
  topP?: number;
  messageLength?: number;
}

export interface CreateAgentRequest {
  name: string;
  description?: string;
  systemPrompt?: string;
  modelId: number;
  allowedTools?: string[];
  allowedKbs?: string[];
  chatOptions?: ChatOptions;
}

export interface UpdateAgentRequest {
  name?: string;
  description?: string;
  systemPrompt?: string;
  modelId?: number;
  allowedTools?: string[];
  allowedKbs?: string[];
  chatOptions?: ChatOptions;
}

export interface CreateAgentResponse {
  agentId: string;
}

export interface AgentVO {
  id: string;
  userId?: number;
  name: string;
  description?: string;
  systemPrompt?: string;
  modelId: number;
  /** 后端通过 model 表补齐展示用模型名，可能为空 */
  modelName?: string;
  allowedTools?: string[];
  allowedKbs?: string[];
  chatOptions?: ChatOptions;
  createdAt?: string;
  updatedAt?: string;
}

export interface GetAgentsResponse {
  agents: AgentVO[];
}

export async function getAgents(): Promise<GetAgentsResponse> {
  return get<GetAgentsResponse>("/api/agents");
}

export async function createAgent(
  request: CreateAgentRequest,
): Promise<CreateAgentResponse> {
  return post<CreateAgentResponse>("/api/agents", request);
}

export async function deleteAgent(agentId: string): Promise<boolean> {
  return del<boolean>(`/api/agents/${agentId}`);
}

export async function updateAgent(
  agentId: string,
  request: UpdateAgentRequest,
): Promise<boolean> {
  return patch<boolean>(`/api/agents/${agentId}`, request);
}

/**
 * 创建聊天会话
 */
export interface CreateChatSessionRequest {
  agentId: string;
  title?: string;
}

export interface CreateChatSessionResponse {
  chatSessionId: string;
}

export async function createChatSession(
  request: CreateChatSessionRequest,
): Promise<CreateChatSessionResponse> {
  return post<CreateChatSessionResponse>("/api/chat-sessions", request);
}

/**
 * 聊天会话相关类型和接口
 */
export interface ChatSessionVO {
  id: string;
  agentId: string;
  title?: string;
  createdAt?: string;
}

export interface GetChatSessionsResponse {
  chatSessions: ChatSessionVO[];
}

export interface GetChatSessionResponse {
  chatSession: ChatSessionVO;
}

export interface UpdateChatSessionRequest {
  title?: string;
}

/**
 * 获取所有聊天会话
 */
export async function getChatSessions(): Promise<GetChatSessionsResponse> {
  return get<GetChatSessionsResponse>("/api/chat-sessions");
}

/**
 * 获取单个聊天会话
 */
export async function getChatSession(
  chatSessionId: string,
): Promise<GetChatSessionResponse> {
  return get<GetChatSessionResponse>(`/api/chat-sessions/${chatSessionId}`);
}

/**
 * 根据 agentId 获取聊天会话
 */
export async function getChatSessionsByAgentId(
  agentId: string,
): Promise<GetChatSessionsResponse> {
  return get<GetChatSessionsResponse>(`/api/chat-sessions/agent/${agentId}`);
}

/**
 * 更新聊天会话
 */
export async function updateChatSession(
  chatSessionId: string,
  request: UpdateChatSessionRequest,
): Promise<boolean> {
  return patch<boolean>(`/api/chat-sessions/${chatSessionId}`, request);
}

/**
 * 删除聊天会话
 */
export async function deleteChatSession(chatSessionId: string): Promise<boolean> {
  return del<boolean>(`/api/chat-sessions/${chatSessionId}`);
}

/**
 * 聊天消息相关类型和接口
 */
export interface MetaData {
  [key: string]: unknown;
}

export interface GetChatMessagesResponse {
  chatMessages: ChatMessageVO[];
}

export interface CreateChatMessageRequest {
  agentId: string;
  sessionId: string;
  role: MessageType;
  content: string;
  metadata?: MetaData;
}

export interface CreateChatMessageResponse {
  chatMessageId: string;
}

export interface UpdateChatMessageRequest {
  content?: string;
  metadata?: MetaData;
}

/**
 * 根据 sessionId 获取聊天消息
 */
export async function getChatMessagesBySessionId(
  sessionId: string,
): Promise<GetChatMessagesResponse> {
  return get<GetChatMessagesResponse>(`/api/chat-messages/session/${sessionId}`);
}

/**
 * 创建聊天消息
 */
export async function createChatMessage(
  request: CreateChatMessageRequest,
): Promise<CreateChatMessageResponse> {
  return post<CreateChatMessageResponse>("/api/chat-messages", request);
}

/**
 * 终止指定会话的 Agent 运行
 */
export async function stopAgent(sessionId: string): Promise<boolean> {
  return post<boolean>(`/api/chat-messages/stop/${sessionId}`, {});
}

/**
 * 更新聊天消息
 */
export async function updateChatMessage(
  chatMessageId: string,
  request: UpdateChatMessageRequest,
): Promise<boolean> {
  return patch<boolean>(`/api/chat-messages/${chatMessageId}`, request);
}

/**
 * 删除聊天消息
 */
export async function deleteChatMessage(chatMessageId: string): Promise<boolean> {
  return del<boolean>(`/api/chat-messages/${chatMessageId}`);
}

/**
 * 知识库相关类型和接口
 */
export interface KnowledgeBaseVO {
  id: string;
  name: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateKnowledgeBaseRequest {
  name: string;
  description?: string;
}

export interface UpdateKnowledgeBaseRequest {
  name?: string;
  description?: string;
}

export interface GetKnowledgeBasesResponse {
  knowledgeBases: KnowledgeBaseVO[];
}

export interface CreateKnowledgeBaseResponse {
  knowledgeBaseId: string;
}

/**
 * 获取所有知识库
 */
export async function getKnowledgeBases(): Promise<GetKnowledgeBasesResponse> {
  return get<GetKnowledgeBasesResponse>("/api/knowledge-bases");
}

/**
 * 创建知识库
 */
export async function createKnowledgeBase(
  request: CreateKnowledgeBaseRequest,
): Promise<CreateKnowledgeBaseResponse> {
  return post<CreateKnowledgeBaseResponse>("/api/knowledge-bases", request);
}

/**
 * 删除知识库
 */
export async function deleteKnowledgeBase(
  knowledgeBaseId: string,
): Promise<boolean> {
  return del<boolean>(`/api/knowledge-bases/${knowledgeBaseId}`);
}

/**
 * 更新知识库
 */
export async function updateKnowledgeBase(
  knowledgeBaseId: string,
  request: UpdateKnowledgeBaseRequest,
): Promise<boolean> {
  return patch<boolean>(`/api/knowledge-bases/${knowledgeBaseId}`, request);
}

/**
 * 文档相关类型和接口
 */
export interface DocumentVO {
  id: string;
  kbId: string;
  filename: string;
  filetype: string;
  size: number;
}

export interface GetDocumentsResponse {
  documents: DocumentVO[];
}

export interface CreateDocumentResponse {
  documentId: string;
}

/**
 * 根据知识库 ID 获取文档列表
 */
export async function getDocumentsByKbId(
  kbId: string,
): Promise<GetDocumentsResponse> {
  return get<GetDocumentsResponse>(`/api/documents/kb/${kbId}`);
}

/**
 * 上传文档
 */
export async function uploadDocument(
  kbId: string,
  file: File,
): Promise<CreateDocumentResponse> {
  const formData = new FormData();
  formData.append("kbId", kbId);
  formData.append("file", file);

  const response = await fetch(`${BASE_URL}/api/documents/upload`, {
    method: "POST",
    body: formData,
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const apiResponse = await response.json();
  if (apiResponse.code !== 200) {
    throw new Error(apiResponse.message || "上传失败");
  }

  return apiResponse.data;
}

/**
 * 删除文档
 */
export async function deleteDocument(documentId: string): Promise<boolean> {
  return del<boolean>(`/api/documents/${documentId}`);
}

/**
 * 工具相关类型和接口
 */
export type ToolType = "FIXED" | "OPTIONAL";

export interface ToolVO {
  name: string;
  description: string;
  type: ToolType;
}

export interface GetOptionalToolsResponse {
  tools: ToolVO[];
}

/**
 * 获取可选工具列表
 */
export async function getOptionalTools(): Promise<GetOptionalToolsResponse> {
  const tools = await get<ToolVO[]>("/api/tools");
  return { tools };
}
