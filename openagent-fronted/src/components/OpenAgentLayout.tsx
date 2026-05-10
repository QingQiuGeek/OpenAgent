import { Routes, Route, useLocation } from "react-router-dom";
import Layout from "../layout/Layout.tsx";
import Sidebar from "../layout/Sidebar.tsx";
import SideMenu from "./SideMenu.tsx";
import Content from "../layout/Content.tsx";
import AgentChatView from "./views/AgentChatView.tsx";
import KnowledgeBaseView from "./views/KnowledgeBaseView.tsx";
import KnowledgeBaseListView from "./views/KnowledgeBaseListView.tsx";
import AdminUsageDashboardView from "./views/AdminUsageDashboardView.tsx";
import MySharesView from "./views/MySharesView.tsx";
import McpServerView from "./views/McpServerView.tsx";
import { SidebarProvider } from "../contexts/SidebarContext.tsx";
import UserMenu from "./auth/UserMenu.tsx";

function PageHeaderUserMenu() {
  const { pathname } = useLocation();
  // 聊天页自带 header，避免重复与遮挡
  const onChatRoute =
    pathname === "/" ||
    pathname === "/agent" ||
    pathname.startsWith("/chat");
  if (onChatRoute) return null;
  return (
    <div className="h-14 shrink-0 border-b border-gray-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 px-4 flex items-center justify-end z-10">
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
          <div className="flex flex-col h-full min-h-0">
            <PageHeaderUserMenu />
            <div className="flex-1 min-h-0 overflow-auto">
            <Routes>
              <Route path="/" element={<AgentChatView />} />
              <Route path="/agent" element={<AgentChatView />} />
              <Route path="/chat" element={<AgentChatView />} />
              <Route path="/chat/:chatSessionId" element={<AgentChatView />} />
              <Route path="/knowledge-base" element={<KnowledgeBaseListView />} />
              <Route
                path="/knowledge-base/:knowledgeBaseId"
                element={<KnowledgeBaseView />}
              />
              <Route path="/usage" element={<AdminUsageDashboardView />} />
              <Route path="/admin/usage" element={<AdminUsageDashboardView />} />
              <Route path="/shares" element={<MySharesView />} />
              <Route path="/mcp-servers" element={<McpServerView />} />
            </Routes>
            </div>
          </div>
        </Content>
      </Layout>
    </SidebarProvider>
  );
}
