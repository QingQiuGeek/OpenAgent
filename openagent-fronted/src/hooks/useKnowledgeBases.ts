import { useCallback, useEffect, useState } from "react";
import {
  createKnowledgeBase,
  type CreateKnowledgeBaseRequest,
  deleteKnowledgeBase,
  getKnowledgeBases,
  updateKnowledgeBase,
  type UpdateKnowledgeBaseRequest,
} from "../api/api.ts";
import type { KnowledgeBase } from "../types";
import { useAuth } from "../contexts/AuthContext.tsx";

export function useKnowledgeBases() {
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  // 是否完成过至少一次拉取，未加载时不应判定为「不存在」
  const [loaded, setLoaded] = useState(false);
  const { user } = useAuth();

  const refreshKnowledgeBases = useCallback(async () => {
    const resp = await getKnowledgeBases();
    const converted: KnowledgeBase[] = resp.knowledgeBases.map((kb) => ({
      knowledgeBaseId: kb.id,
      name: kb.name,
      description: kb.description || "",
      createdAt: kb.createdAt,
      updatedAt: kb.updatedAt,
    }));
    setKnowledgeBases(converted);
    setLoaded(true);
  }, []);

  useEffect(() => {
    if (!user) {
      setKnowledgeBases([]);
      setLoaded(false);
      return;
    }
    refreshKnowledgeBases().then();
  }, [user, refreshKnowledgeBases]);

  const createKnowledgeBaseHandle = useCallback(
    async (request: CreateKnowledgeBaseRequest) => {
      await createKnowledgeBase(request);
      await refreshKnowledgeBases();
    },
    [refreshKnowledgeBases],
  );

  const updateKnowledgeBaseHandle = useCallback(
    async (knowledgeBaseId: string, request: UpdateKnowledgeBaseRequest) => {
      await updateKnowledgeBase(knowledgeBaseId, request);
      await refreshKnowledgeBases();
    },
    [refreshKnowledgeBases],
  );

  const deleteKnowledgeBaseHandle = useCallback(
    async (knowledgeBaseId: string) => {
      await deleteKnowledgeBase(knowledgeBaseId);
      await refreshKnowledgeBases();
    },
    [refreshKnowledgeBases],
  );

  return {
    knowledgeBases,
    loaded,
    refreshKnowledgeBases,
    createKnowledgeBaseHandle,
    updateKnowledgeBaseHandle,
    deleteKnowledgeBaseHandle,
  };
}

