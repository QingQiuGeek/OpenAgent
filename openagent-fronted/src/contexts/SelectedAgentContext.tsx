import React, { createContext, useCallback, useContext, useEffect, useState } from "react";

interface SelectedAgentContextValue {
  selectedAgentId: string | null;
  selectAgent: (agentId: string | null) => void;
}

const SelectedAgentContext = createContext<SelectedAgentContextValue | undefined>(undefined);

const STORAGE_KEY = "openagent.selectedAgentId";

export function SelectedAgentProvider({ children }: { children: React.ReactNode }) {
  const [selectedAgentId, setSelectedAgentId] = useState<string | null>(() => {
    try {
      return localStorage.getItem(STORAGE_KEY);
    } catch {
      return null;
    }
  });

  const selectAgent = useCallback((agentId: string | null) => {
    setSelectedAgentId(agentId);
  }, []);

  useEffect(() => {
    try {
      if (selectedAgentId) localStorage.setItem(STORAGE_KEY, selectedAgentId);
      else localStorage.removeItem(STORAGE_KEY);
    } catch {
      // ignore storage errors
    }
  }, [selectedAgentId]);

  return (
    <SelectedAgentContext.Provider value={{ selectedAgentId, selectAgent }}>
      {children}
    </SelectedAgentContext.Provider>
  );
}

export function useSelectedAgent(): SelectedAgentContextValue {
  const ctx = useContext(SelectedAgentContext);
  if (!ctx) throw new Error("useSelectedAgent 必须在 SelectedAgentProvider 中使用");
  return ctx;
}
