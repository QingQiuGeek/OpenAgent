import { useCallback, useEffect, useState } from "react";
import {
  type CreateModelRequest,
  type UpdateModelRequest,
  type ModelVO,
  createModel,
  updateModel,
  deleteModel,
  getModels,
} from "../api/api.ts";
import { useAuth } from "../contexts/AuthContext.tsx";

export function useModels() {
  const [models, setModels] = useState<ModelVO[]>([]);
  const [loading, setLoading] = useState(false);
  const { user } = useAuth();

  const refresh = useCallback(async () => {
    if (!user) {
      setModels([]);
      return;
    }
    setLoading(true);
    try {
      const resp = await getModels();
      setModels(resp.models);
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  /** 创建新模型并返回新 modelId，便于调用方自动选中 */
  const createModelHandle = useCallback(
    async (body: CreateModelRequest): Promise<number> => {
      const resp = await createModel(body);
      await refresh();
      return resp.modelId;
    },
    [refresh],
  );

  const updateModelHandle = useCallback(
    async (modelId: number, body: UpdateModelRequest): Promise<void> => {
      await updateModel(modelId, body);
      await refresh();
    },
    [refresh],
  );

  const deleteModelHandle = useCallback(
    async (modelId: number): Promise<void> => {
      await deleteModel(modelId);
      await refresh();
    },
    [refresh],
  );

  return { models, loading, refresh, createModelHandle, updateModelHandle, deleteModelHandle };
}
