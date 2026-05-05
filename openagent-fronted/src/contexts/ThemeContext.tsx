import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { ConfigProvider, theme as antdTheme } from "antd";

type ThemeMode = "light" | "dark";

interface ThemeContextValue {
  mode: ThemeMode;
  setMode: (m: ThemeMode) => void;
  toggle: () => void;
}

const STORAGE_KEY = "openagent.theme";

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [mode, setModeState] = useState<ThemeMode>(() => {
    try {
      const v = localStorage.getItem(STORAGE_KEY);
      if (v === "dark" || v === "light") return v;
    } catch {
      // ignore
    }
    return "light";
  });

  const setMode = useCallback((m: ThemeMode) => setModeState(m), []);
  const toggle = useCallback(
    () => setModeState((m) => (m === "dark" ? "light" : "dark")),
    [],
  );

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, mode);
    } catch {
      // ignore
    }
    const root = document.documentElement;
    // 主题切换时临时打开全局颜色过渡，避免闪光；切换结束后移除，避免影响交互
    root.classList.add("theme-transitioning");
    if (mode === "dark") root.classList.add("dark");
    else root.classList.remove("dark");
    const t = window.setTimeout(() => {
      root.classList.remove("theme-transitioning");
    }, 320);
    return () => window.clearTimeout(t);
  }, [mode]);

  const value = useMemo(() => ({ mode, setMode, toggle }), [mode, setMode, toggle]);

  return (
    <ThemeContext.Provider value={value}>
      <ConfigProvider
        theme={{
          algorithm:
            mode === "dark" ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
        }}
      >
        {children}
      </ConfigProvider>
    </ThemeContext.Provider>
  );
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useTheme 必须在 ThemeProvider 中使用");
  return ctx;
}
