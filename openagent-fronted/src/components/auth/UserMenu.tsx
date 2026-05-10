import React from "react";
import { Avatar, Button, Dropdown, type MenuProps } from "antd";
import {
  UserOutlined,
  LogoutOutlined,
  DashboardOutlined,
  ShareAltOutlined,
  ApiOutlined,
  HomeOutlined,
  BookOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../contexts/AuthContext.tsx";

/**
 * 左上角用户入口：未登录显示"登录/注册"按钮；已登录显示头像 + 下拉菜单。
 */
const UserMenu: React.FC = () => {
  const { user, openAuthModal, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) {
    return (
      <Button type="primary" size="small" onClick={openAuthModal}>
        登录 / 注册
      </Button>
    );
  }

  const items: MenuProps["items"] = [
    {
      key: "info",
      label: (
        <div className="px-1 py-0.5">
          <div className="text-sm font-medium text-gray-900">
            {user.userName || "用户"}
          </div>
          {user.mail && (
            <div className="text-xs text-gray-500">{user.mail}</div>
          )}
        </div>
      ),
      disabled: true,
    },
    { type: "divider" },
    {
      key: "home",
      icon: <HomeOutlined />,
      label: "首页",
      onClick: () => navigate("/"),
    },
    {
      key: "knowledge-base",
      icon: <BookOutlined />,
      label: "知识库",
      onClick: () => navigate("/knowledge-base"),
    },
    {
      key: "my-shares",
      icon: <ShareAltOutlined />,
      label: "我的分享",
      onClick: () => navigate("/shares"),
    },
    {
      key: "mcp-servers",
      icon: <ApiOutlined />,
      label: "MCP 服务器",
      onClick: () => navigate("/mcp-servers"),
    },
    {
      key: "my-models",
      icon: <DashboardOutlined />,
      label: "我的模型",
      onClick: () => navigate("/usage"),
    },
    { type: "divider" },
    {
      key: "logout",
      icon: <LogoutOutlined />,
      label: "退出登录",
      onClick: async () => {
        await logout();
        navigate("/", { replace: true });
      },
    },
  ];

  return (
    <Dropdown menu={{ items }} placement="bottomLeft" trigger={["click"]}>
      <div className="flex items-center gap-2 cursor-pointer select-none">
        <Avatar size="small" icon={<UserOutlined />} />
        <span className="text-sm text-gray-700 max-w-[80px] truncate">
          {user.userName || "用户"}
        </span>
      </div>
    </Dropdown>
  );
};

export default UserMenu;
