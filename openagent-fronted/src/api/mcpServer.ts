import { del, get, patch, post } from "./http.ts";

export interface McpServerVO {
  id: number;
  name: string;
  description?: string;
  /** 与后端 mcp_transport 枚举一致：stdio / sse / http */
  transport: string;
  command?: string;
  url?: string;
  /** JSON 字符串 */
  env?: string;
  /** JSON 字符串 */
  headers?: string;
  enabled: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateMcpServerRequest {
  name: string;
  description?: string;
  transport: string;
  command?: string;
  url?: string;
  env?: string;
  headers?: string;
  enabled?: number;
}

export type UpdateMcpServerRequest = Partial<CreateMcpServerRequest>;

export interface GetMcpServersResponse {
  mcpServers: McpServerVO[];
}

export interface CreateMcpServerResponse {
  mcpServerId: number;
}

export interface McpToolGroupVO {
  mcpServerId: number;
  mcpServerName: string;
  transport: string;
  toolNames: string[];
  ok: boolean;
  errorMsg?: string;
}

export async function listMcpServers(): Promise<McpServerVO[]> {
  const resp = await get<GetMcpServersResponse>("/api/mcp-servers");
  return resp.mcpServers ?? [];
}

export async function createMcpServer(body: CreateMcpServerRequest): Promise<CreateMcpServerResponse> {
  return post<CreateMcpServerResponse>("/api/mcp-servers", body);
}

export async function updateMcpServer(
  mcpServerId: number,
  body: UpdateMcpServerRequest,
): Promise<boolean> {
  return patch<boolean>(`/api/mcp-servers/${mcpServerId}`, body);
}

export async function deleteMcpServer(mcpServerId: number): Promise<boolean> {
  return del<boolean>(`/api/mcp-servers/${mcpServerId}`);
}

/** 测试连接：成功返回工具名列表；失败抛 BizException 由 http.ts toast */
export async function testMcpServerConnection(mcpServerId: number): Promise<string[]> {
  return post<string[]>(`/api/mcp-servers/${mcpServerId}/test`, {});
}

/** Agent 编辑器使用：列出当前用户已启用 MCP 的工具分组 */
export async function listMyMcpToolGroups(): Promise<McpToolGroupVO[]> {
  return get<McpToolGroupVO[]>("/api/tools/mcp");
}
