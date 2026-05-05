import { Routes, Route, useLocation } from "react-router-dom";
import Layout from "../layout/Layout.tsx";
import Sidebar from "../layout/Sidebar.tsx";
import SideMenu from "./SideMenu.tsx";
import Content from "../layout/Content.tsx";
import AgentChatView from "./views/AgentChatView.tsx";
import KnowledgeBaseView from "./views/KnowledgeBaseView.tsx";
import { SidebarProvider } from "../contexts/SidebarContext.tsx";
import UserMenu from "./auth/UserMenu.tsx";

function FloatingUserMenu() {
  const { pathname } = useLocation();
  // 聊天页自带 header，避免重复与遮挡
  const onChatRoute =
    pathname === "/" ||
    pathname === "/agent" ||
    pathname.startsWith("/chat");
  if (onChatRoute) return null;
  return (
    <div className="absolute top-3 right-4 z-10">
      <UserMenu />
    </div>
  );
}

export default function OpenAgentLayout() {
  return (
    <SidebarProvider>
      <Layout>
        <Sidebar>
          <SideMenu />
        </Sidebar>
        <Content>
          <div className="relative h-full">
            <FloatingUserMenu />
            <Routes>
              <Route path="/" element={<AgentChatView />} />
              <Route path="/agent" element={<AgentChatView />} />
              <Route path="/chat" element={<AgentChatView />} />
              <Route path="/chat/:chatSessionId" element={<AgentChatView />} />
              <Route path="/knowledge-base" element={<KnowledgeBaseView />} />
              <Route
                path="/knowledge-base/:knowledgeBaseId"
                element={<KnowledgeBaseView />}
              />
            </Routes>
          </div>
        </Content>
      </Layout>
    </SidebarProvider>
  );
}
