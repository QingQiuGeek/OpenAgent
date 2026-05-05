import React from "react";
import { Button, Tooltip } from "antd";
import { MenuUnfoldOutlined } from "@ant-design/icons";
import { useSidebar } from "../contexts/SidebarContext.tsx";

interface SidebarProps {
  children: React.ReactNode;
}

const Sidebar: React.FC<SidebarProps> = ({ children }) => {
  const { collapsed, toggle } = useSidebar();
  return (
    <div
      className="h-full bg-slate-50 dark:bg-zinc-900 border-r border-gray-200 dark:border-zinc-700 overflow-hidden transition-all duration-300 ease-in-out shrink-0"
      style={{ width: collapsed ? 48 : 320 }}
    >
      {collapsed ? (
        <div className="h-full w-12 flex flex-col items-center pt-3">
          <Tooltip title="展开侧边栏" placement="right">
            <Button
              type="text"
              icon={<MenuUnfoldOutlined />}
              onClick={toggle}
              aria-label="展开侧边栏"
            />
          </Tooltip>
        </div>
      ) : (
        <div style={{ width: 320 }} className="h-full">
          {children}
        </div>
      )}
    </div>
  );
};

export default Sidebar;
