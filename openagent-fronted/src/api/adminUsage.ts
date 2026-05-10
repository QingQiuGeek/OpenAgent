import { get } from "./http.ts";

export interface UsageOverviewVO {
  totalCalls: number;
  totalTokens: number;
  totalPromptTokens: number;
  totalCompletionTokens: number;
  avgLatencyMs: number;
  errorCalls: number;
  errorRate: number;
}

export interface DailyUsageVO {
  date: string;
  calls: number;
  totalTokens: number;
}

export interface ModelUsageVO {
  modelId: number;
  modelName: string;
  calls: number;
  totalTokens: number;
  avgLatencyMs: number;
  errorCalls: number;
}

export interface AgentUsageLogVO {
  id: number;
  userId: number;
  agentId?: string;
  sessionId?: string;
  modelId?: number;
  chatMode?: string;
  promptTokens?: number;
  completionTokens?: number;
  totalTokens?: number;
  latencyMs?: number;
  status?: string;
  errorMsg?: string;
  createdAt?: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
}

export interface DateRangeParams {
  from?: string; // yyyy-MM-dd
  to?: string;
}

function qs(params: object): string {
  const entries = Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== "");
  if (!entries.length) return "";
  const sp = new URLSearchParams();
  entries.forEach(([k, v]) => sp.append(k, String(v)));
  return `?${sp.toString()}`;
}

export async function getUsageOverview(p: DateRangeParams = {}): Promise<UsageOverviewVO> {
  return get<UsageOverviewVO>(`/api/usage/overview${qs(p)}`);
}

export async function getUsageDaily(p: DateRangeParams = {}): Promise<DailyUsageVO[]> {
  return get<DailyUsageVO[]>(`/api/usage/daily${qs(p)}`);
}

export async function getUsageByModel(p: DateRangeParams = {}): Promise<ModelUsageVO[]> {
  return get<ModelUsageVO[]>(`/api/usage/by-model${qs(p)}`);
}

export async function getUsageList(
  p: DateRangeParams & { page?: number; pageSize?: number; status?: string } = {}
): Promise<PageResult<AgentUsageLogVO>> {
  return get<PageResult<AgentUsageLogVO>>(`/api/usage/list${qs(p)}`);
}
