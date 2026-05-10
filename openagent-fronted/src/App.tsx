import { BrowserRouter, Route, Routes } from "react-router-dom";
import { Spin } from "antd";
import OpenAgentLayout from "./components/OpenAgentLayout.tsx";
import { ChatSessionsProvider } from "./contexts/ChatSessionsContext.tsx";
import { AuthProvider, useAuth } from "./contexts/AuthContext.tsx";
import { SelectedAgentProvider } from "./contexts/SelectedAgentContext.tsx";
import { AgentsProvider } from "./contexts/AgentsContext.tsx";
import LoginRegisterModal from "./components/auth/LoginRegisterModal.tsx";
import SharedChatView from "./components/views/SharedChatView.tsx";

/**
 * 不论登录态如何，都渲染主布局；以 user.userId 作 key，切换/登出时强制子树重挂，清理旧缓存。
 * 数据 hook 内部会根据用户态决定是否拉数据；登录提示由具体动作（如点发送）触发。
 */
function AppBody() {
  const { user, initializing } = useAuth();

  if (initializing) {
    return (
      <div className="h-screen flex items-center justify-center">
        <Spin />
      </div>
    );
  }

  return (
    <ChatSessionsProvider key={user?.userId ?? "guest"}>
      <AgentsProvider>
        <SelectedAgentProvider>
          <OpenAgentLayout />
        </SelectedAgentProvider>
      </AgentsProvider>
    </ChatSessionsProvider>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* 公开分享页：不进 AuthProvider，避免触发用户态相关上下文与守卫 */}
        <Route path="/share/:slug" element={<SharedChatView />} />
        <Route
          path="*"
          element={
            <AuthProvider>
              <AppBody />
              <LoginRegisterModal />
            </AuthProvider>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
