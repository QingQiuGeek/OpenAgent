import { useCallback, useEffect, useRef, useState } from "react";
import { getEnumValues } from "../api/api";

/** 模块级缓存：避免同一类别重复 fetch。 */
const cache = new Map<string, string[]>();
const inflight = new Map<string, Promise<string[]>>();

function fetchEnum(type: string, force = false): Promise<string[]> {
  if (!force) {
    const cached = cache.get(type);
    if (cached) return Promise.resolve(cached);
    const pending = inflight.get(type);
    if (pending) return pending;
  } else {
    cache.delete(type);
    inflight.delete(type);
  }
  const p = getEnumValues(type)
    .then((list) => {
      cache.set(type, list);
      return list;
    })
    .finally(() => inflight.delete(type));
  inflight.set(type, p);
  return p;
}

/**
 * 拉取某类别下的字典值数组。
 * - 默认 enabled=true，挂载时立即拉取（向下兼容旧用法）；
 * - 传 enabled=false 则不主动请求，调用方需要时调 reload() 触发；
 * - reload() 会清掉缓存强制走最新数据，便于在下拉打开时刷新。
 */
export function useEnumOptions(type: string, enabled: boolean = true) {
  const [options, setOptions] = useState<string[]>(() => cache.get(type) ?? []);
  const [loading, setLoading] = useState(false);
  const aliveRef = useRef(true);

  useEffect(() => {
    aliveRef.current = true;
    return () => {
      aliveRef.current = false;
    };
  }, []);

  const reload = useCallback(
    (force: boolean = true) => {
      setLoading(true);
      return fetchEnum(type, force)
        .then((list) => {
          if (aliveRef.current) setOptions(list);
          return list;
        })
        .finally(() => {
          if (aliveRef.current) setLoading(false);
        });
    },
    [type],
  );

  useEffect(() => {
    if (!enabled) return;
    const cached = cache.get(type);
    if (cached) {
      setOptions(cached);
      return;
    }
    reload(false);
  }, [type, enabled, reload]);

  return { options, loading, reload };
}

/** 强制清空指定 type（或全部）的内存缓存，用于管理后台编辑后立即生效。 */
export function invalidateEnumCache(type?: string) {
  if (type) {
    cache.delete(type);
  } else {
    cache.clear();
  }
}
