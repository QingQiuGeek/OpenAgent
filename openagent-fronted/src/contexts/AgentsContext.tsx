import React, { createContext, useCallback, useContext, useEffect, useState } from "react";
import {
  type AgentVO,
  createAgent,
  type CreateAgentRequest,
  deleteAgent,
  getAgents,
  updateAgent,
  type UpdateAgentRequest,
} from "../api/api.ts";
import { useAuth } from "./AuthContext.tsx";

/**
 * 全局 Agents 列表 + CRUD 操作。
 * <p>之前 {@code useAgents} 是无 Provider 的组件本地 hook，每个调用点持有独立 state。
 * SideMenu 创建/编辑 agent 后只刷新自己那一份，
 * AgentChatView 那边 agents 永远是旧值，导致新建 agent 后 header 显示为空。
 * 改为 Context 后所有消费者共享同一份 state，一处刷新全局生效。
 */
interface AgentsContextValue {
  agents: AgentVO[];
  refreshAgents: () => Promise<void>;
  createAgentHandle: (request: CreateAgentRequest) => Promise<void>;
  deleteAgentHandle: (agentId: string) => Promise<void>;
  updateAgentHandle: (agentId: string, request: UpdateAgentRequest) => Promise<void>;
}

const AgentsContext = createContext<AgentsContextValue | undefined>(undefined);

export function AgentsProvider({ children }: { children: React.ReactNode }) {
  const [agents, setAgents] = useState<AgentVO[]>([]);
  const { user } = useAuth();

  const refreshAgents = useCallback(async () => {
    if (!user) return;
    const resp = await getAgents();
    setAgents(resp.agents);
  }, [user]);

  useEffect(() => {
    if (!user) {
      setAgents([]);
      return;
    }
    refreshAgents().catch((err) => console.error("[AgentsProvider] 拉取 agents 失败:", err));
  }, [user, refreshAgents]);

  const createAgentHandle = useCallback(
    async (request: CreateAgentRequest) => {
      await createAgent(request);
      await refreshAgents();
    },
    [refreshAgents],
  );

  const deleteAgentHandle = useCallback(
    async (agentId: string) => {
      await deleteAgent(agentId);
      await refreshAgents();
    },
    [refreshAgents],
  );

  const updateAgentHandle = useCallback(
    async (agentId: string, request: UpdateAgentRequest) => {
      await updateAgent(agentId, request);
      await refreshAgents();
    },
    [refreshAgents],
  );

  return (
    <AgentsContext.Provider
      value={{ agents, refreshAgents, createAgentHandle, deleteAgentHandle, updateAgentHandle }}
    >
      {children}
    </AgentsContext.Provider>
  );
}

/**
 * 兼容旧 API：保持 {@code useAgents()} 名称与返回结构一致，
 * 这样所有现有调用点（SideMenu / AgentChatView 等）无需改动。
 */
export function useAgents(): AgentsContextValue {
  const ctx = useContext(AgentsContext);
  if (!ctx) throw new Error("useAgents 必须在 AgentsProvider 中使用");
  return ctx;
}
