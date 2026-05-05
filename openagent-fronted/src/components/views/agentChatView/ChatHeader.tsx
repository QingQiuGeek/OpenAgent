import React from "react";
import { Button, Tag, Tooltip } from "antd";
import { MoonOutlined, SunOutlined } from "@ant-design/icons";
import UserMenu from "../../auth/UserMenu.tsx";
import { useTheme } from "../../../contexts/ThemeContext.tsx";

interface ChatHeaderProps {
  title?: string;
  subtitle?: string;
  description?: string;
}

/**
 * 聊天区域顶部 header：左侧展示当前智能体/会话标题，右侧放主题切换 + 用户菜单。
 */
const ChatHeader: React.FC<ChatHeaderProps> = ({ title, subtitle, description }) => {
  const { mode, toggle } = useTheme();
  const isDark = mode === "dark";
  return (
    <div className="h-14 shrink-0 border-b border-gray-200 bg-white dark:bg-zinc-800 dark:border-zinc-700 px-4 flex items-center justify-between z-10">
      <div className="flex items-center gap-2.5 min-w-0 flex-1 pr-4">
        {title && (
          <span className="text-xl font-extrabold text-gray-900 dark:text-gray-100 truncate tracking-tight">
            {title}
          </span>
        )}
        {subtitle && (
          <Tag
            color="geekblue"
            className="!m-0 !text-sm !px-2 !py-0.5 !leading-5 !font-bold shrink-0"
          >
            {subtitle}
          </Tag>
        )}
        {description && (
          <span className="text-sm text-gray-400 dark:text-gray-500 shrink-0 max-w-[320px] truncate">
            {description.length > 50 ? description.slice(0, 50) + "…" : description}
          </span>
        )}
      </div>
      <div className="flex items-center gap-3 shrink-0">
        <Tooltip title={isDark ? "切换到浅色" : "切换到深色"}>
          <Button
            type="text"
            shape="circle"
            size="large"
            aria-label="切换主题"
            onClick={toggle}
            icon={
              isDark ? (
                <SunOutlined className="text-amber-400" />
              ) : (
                <MoonOutlined className="text-indigo-500" />
              )
            }
          />
        </Tooltip>
        <UserMenu />
      </div>
    </div>
  );
};

export default ChatHeader;
