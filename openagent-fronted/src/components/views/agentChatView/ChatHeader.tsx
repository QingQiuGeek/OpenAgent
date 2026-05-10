import React from "react";
import { Button, Dropdown, Empty, Tag, Tooltip, Typography } from "antd";
import {
  ApiOutlined,
  AppstoreOutlined,
  BookOutlined,
  DownOutlined,
  MoonOutlined,
  SunOutlined,
} from "@ant-design/icons";
import UserMenu from "../../auth/UserMenu.tsx";
import { useTheme } from "../../../contexts/ThemeContext.tsx";

/** Header 下拉列表单项：仅展示，不带任何操作 */
export interface HeaderListItem {
  /** 必填，列表主文本 */
  name: string;
  /** 可选描述，以次要色阶展示 */
  description?: string;
}

interface ChatHeaderProps {
  title?: string;
  subtitle?: string;
  description?: string;
  /** 当前 agent 绑定的工具（已解析为名称），未提供时不渲染按钮 */
  tools?: HeaderListItem[];
  /** 当前 agent 绑定的 MCP 服务器（name=服务名，description=工具清单） */
  mcpServers?: HeaderListItem[];
  /** 当前 agent 绑定的知识库，同上 */
  knowledgeBases?: HeaderListItem[];
}

/**
 * 抽出公用下拉试图：只读列表。未配置时展示 Empty。
 * <p>使用 antd {@link Dropdown}的 popupRender（新版的 dropdownRender）手动提供面板内容，
 * 避免 menu items 中一只能填文本导致描述丢失。
 */
const ConfigDropdown: React.FC<{
  icon: React.ReactNode;
  label: string;
  items: HeaderListItem[];
}> = ({ icon, label, items }) => {
  const count = items.length;
  return (
    <Dropdown
      trigger={["click"]}
      placement="bottomLeft"
      popupRender={() => (
        <div className="min-w-[220px] max-w-[360px] rounded-md bg-white dark:bg-zinc-800 shadow-lg border border-gray-200 dark:border-zinc-700 py-1">
          {count === 0 ? (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="未配置"
              className="!my-3"
            />
          ) : (
            <ul className="max-h-[320px] overflow-y-auto m-0 p-0 list-none">
              {items.map((it) => (
                <li
                  key={it.name}
                  className="px-3 py-2 hover:bg-gray-50 dark:hover:bg-zinc-700/60"
                >
                  <Typography.Text strong className="!text-sm block truncate">
                    {it.name}
                  </Typography.Text>
                  {it.description && (
                    <Typography.Text
                      type="secondary"
                      className="!text-xs block !leading-snug mt-0.5"
                    >
                      {it.description}
                    </Typography.Text>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    >
      <Button type="text" size="small" className="!flex !items-center !gap-1">
        {icon}
        <span>
          {label}
          <span className="ml-1 text-gray-400">({count})</span>
        </span>
        <DownOutlined className="!text-[10px]" />
      </Button>
    </Dropdown>
  );
};

/**
 * 聊天区域顶部 header：左侧展示当前智能体/会话标题，右侧放主题切换 + 用户菜单。
 */
const ChatHeader: React.FC<ChatHeaderProps> = ({
  title,
  subtitle,
  description,
  tools,
  mcpServers,
  knowledgeBases,
}) => {
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
        {tools && (
          <ConfigDropdown
            icon={<ApiOutlined />}
            label="Tool列表"
            items={tools}
          />
        )}
        {mcpServers && (
          <ConfigDropdown
            icon={<AppstoreOutlined />}
            label="MCP 列表"
            items={mcpServers}
          />
        )}
        {knowledgeBases && (
          <ConfigDropdown
            icon={<BookOutlined />}
            label="知识库列表"
            items={knowledgeBases}
          />
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
