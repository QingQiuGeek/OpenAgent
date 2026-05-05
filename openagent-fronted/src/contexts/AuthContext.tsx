import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";
import { message } from "antd";
import {
  login as apiLogin,
  register as apiRegister,
  logout as apiLogout,
  sendRegisterCode as apiSendRegisterCode,
  getLoginUser,
  type LoginUserVO,
  type UserLoginDTO,
  type UserRegisterDTO,
} from "../api/api.ts";

interface AuthContextValue {
  user: LoginUserVO | null;
  initializing: boolean;
  authModalOpen: boolean;
  openAuthModal: () => void;
  closeAuthModal: () => void;
  /** 已登录返回 true；未登录弹出登录框并返回 false（调用方据此中断后续逻辑）。 */
  requireAuth: () => boolean;
  login: (body: UserLoginDTO) => Promise<void>;
  register: (body: UserRegisterDTO) => Promise<void>;
  sendRegisterCode: (mail: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<LoginUserVO | null>(null);
  const [initializing, setInitializing] = useState(true);
  const [authModalOpen, setAuthModalOpen] = useState(false);

  const openAuthModal = useCallback(() => setAuthModalOpen(true), []);
  const closeAuthModal = useCallback(() => setAuthModalOpen(false), []);

  const requireAuth = useCallback(() => {
    if (user) return true;
    setAuthModalOpen(true);
    return false;
  }, [user]);

  // 启动时尝试拉当前用户（由浏览器 sa-token cookie 自动带身份）
  useEffect(() => {
    let cancelled = false;
    async function bootstrap() {
      try {
        const u = await getLoginUser();
        if (!cancelled) setUser(u);
      } catch {
        // 未登录或 cookie 失效：静默忽略，按游客处理
      } finally {
        if (!cancelled) setInitializing(false);
      }
    }
    bootstrap();
    return () => {
      cancelled = true;
    };
  }, []);

  // 监听 http.ts 广播的"未登录"事件：仅当之前是登录状态（说明 token 过期）时才自动弹登录框；
  // 未登录用户的 401 静默忽略，避免一打开页面就弹窗。
  useEffect(() => {
    const handler = () => {
      setUser((prev) => {
        if (prev) {
          message.warning("登录已过期，请重新登录");
          setAuthModalOpen(true);
        }
        return null;
      });
    };
    window.addEventListener("auth:unauthorized", handler);
    return () => window.removeEventListener("auth:unauthorized", handler);
  }, []);

  const login = useCallback(async (body: UserLoginDTO) => {
    const vo = await apiLogin(body);
    setUser(vo);
    message.success("登录成功");
    setAuthModalOpen(false);
  }, []);

  const register = useCallback(async (body: UserRegisterDTO) => {
    const vo = await apiRegister(body);
    setUser(vo);
    message.success("注册成功");
    setAuthModalOpen(false);
  }, []);

  const sendRegisterCode = useCallback(async (mail: string) => {
    await apiSendRegisterCode({ mail });
    message.success("验证码已发送，请注意查收邮箱");
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiLogout();
    } catch {
      // 忽略服务端错误，前端清本地状态
    }
    setUser(null);
    message.success("已退出登录");
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        initializing,
        authModalOpen,
        openAuthModal,
        closeAuthModal,
        requireAuth,
        login,
        register,
        sendRegisterCode,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth 必须在 AuthProvider 中使用");
  return ctx;
}
