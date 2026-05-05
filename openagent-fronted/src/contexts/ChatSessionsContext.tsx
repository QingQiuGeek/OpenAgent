import React, { createContext, useContext, useEffect, useState, useCallback } from "react";
import {
  type ChatSessionVO,
  getChatSessions,
  deleteChatSession,
} from "../api/api.ts";
import { useAuth } from "./AuthContext.tsx";

interface ChatSessionsContextType {
  chatSessions: ChatSessionVO[];
  loading: boolean;
  refreshChatSessions: () => Promise<void>;
  deleteChatSession: (chatSessionId: string) => Promise<void>;
}

const ChatSessionsContext = createContext<ChatSessionsContextType | undefined>(
  undefined
);

export function ChatSessionsProvider({ children }: { children: React.ReactNode }) {
  const [chatSessions, setChatSessions] = useState<ChatSessionVO[]>([]);
  const [loading, setLoading] = useState(false);
  const { user } = useAuth();

  const fetchChatSessions = useCallback(async () => {
    if (!user) {
      setChatSessions([]);
      return;
    }
    setLoading(true);
    try {
      const resp = await getChatSessions();
      setChatSessions(resp.chatSessions);
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    fetchChatSessions();
  }, [fetchChatSessions]);

  const deleteChatSessionHandle = useCallback(async (chatSessionId: string) => {
    await deleteChatSession(chatSessionId);
    await fetchChatSessions();
  }, [fetchChatSessions]);

  return (
    <ChatSessionsContext.Provider
      value={{
        chatSessions,
        loading,
        refreshChatSessions: fetchChatSessions,
        deleteChatSession: deleteChatSessionHandle,
      }}
    >
      {children}
    </ChatSessionsContext.Provider>
  );
}

export function useChatSessionsContext() {
  const context = useContext(ChatSessionsContext);
  if (context === undefined) {
    throw new Error(
      "useChatSessionsContext must be used within a ChatSessionsProvider"
    );
  }
  return context;
}

