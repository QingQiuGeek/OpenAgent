import React from "react";
import { Avatar, Button, Dropdown, type MenuProps } from "antd";
import { UserOutlined, LogoutOutlined } from "@ant-design/icons";
import { useAuth } from "../../contexts/AuthContext.tsx";

/**
 * 左上角用户入口：未登录显示"登录/注册"按钮；已登录显示头像 + 下拉菜单。
 */
const UserMenu: React.FC = () => {
  const { user, openAuthModal, logout } = useAuth();

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
      key: "logout",
      icon: <LogoutOutlined />,
      label: "退出登录",
      onClick: () => {
        logout();
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
