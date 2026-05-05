import React, { useEffect, useState } from "react";
import { Button, Form, Input, Modal, Tabs, message } from "antd";
import { MailOutlined, LockOutlined, SafetyOutlined } from "@ant-design/icons";
import { useAuth } from "../../contexts/AuthContext.tsx";

type TabKey = "login" | "register";

interface LoginFormValues {
  mail: string;
  password: string;
}

interface RegisterFormValues {
  mail: string;
  password: string;
  rePassword: string;
  code: string;
}

const CODE_COUNTDOWN = 60;

const LoginRegisterModal: React.FC = () => {
  const { authModalOpen, closeAuthModal, login, register, sendRegisterCode } =
    useAuth();
  const [activeTab, setActiveTab] = useState<TabKey>("login");
  const [loginLoading, setLoginLoading] = useState(false);
  const [registerLoading, setRegisterLoading] = useState(false);
  const [codeLoading, setCodeLoading] = useState(false);
  const [codeCountdown, setCodeCountdown] = useState(0);

  const [loginForm] = Form.useForm<LoginFormValues>();
  const [registerForm] = Form.useForm<RegisterFormValues>();

  // 倒计时
  useEffect(() => {
    if (codeCountdown <= 0) return;
    const t = setTimeout(() => setCodeCountdown((v) => v - 1), 1000);
    return () => clearTimeout(t);
  }, [codeCountdown]);

  // 每次打开重置
  useEffect(() => {
    if (!authModalOpen) {
      loginForm.resetFields();
      registerForm.resetFields();
      setActiveTab("login");
    }
  }, [authModalOpen, loginForm, registerForm]);

  const handleLogin = async (values: LoginFormValues) => {
    setLoginLoading(true);
    try {
      await login(values);
    } catch (e) {
      // 错误消息在 http.ts 已统一弹出
    } finally {
      setLoginLoading(false);
    }
  };

  const handleRegister = async (values: RegisterFormValues) => {
    if (values.password !== values.rePassword) {
      message.error("两次密码输入不一致");
      return;
    }
    setRegisterLoading(true);
    try {
      await register(values);
    } catch (e) {
      // 同上
    } finally {
      setRegisterLoading(false);
    }
  };

  const handleSendCode = async () => {
    try {
      const mail = await registerForm.validateFields(["mail"]);
      setCodeLoading(true);
      await sendRegisterCode(mail.mail);
      setCodeCountdown(CODE_COUNTDOWN);
    } catch (e) {
      // 表单校验失败或接口错误
    } finally {
      setCodeLoading(false);
    }
  };

  return (
    <Modal
      open={authModalOpen}
      onCancel={closeAuthModal}
      footer={null}
      destroyOnHidden
      maskClosable
      width={420}
      centered
      title={null}
    >
      <Tabs
        activeKey={activeTab}
        onChange={(k) => setActiveTab(k as TabKey)}
        centered
        items={[
          {
            key: "login",
            label: "登录",
            children: (
              <Form
                form={loginForm}
                layout="vertical"
                onFinish={handleLogin}
                autoComplete="off"
              >
                <Form.Item
                  name="mail"
                  label="邮箱"
                  rules={[
                    { required: true, message: "请输入邮箱" },
                    { type: "email", message: "邮箱格式不正确" },
                  ]}
                >
                  <Input prefix={<MailOutlined />} placeholder="you@example.com" />
                </Form.Item>
                <Form.Item
                  name="password"
                  label="密码"
                  rules={[{ required: true, message: "请输入密码" }]}
                >
                  <Input.Password prefix={<LockOutlined />} placeholder="密码" />
                </Form.Item>
                <Button
                  type="primary"
                  htmlType="submit"
                  block
                  loading={loginLoading}
                >
                  登录
                </Button>
              </Form>
            ),
          },
          {
            key: "register",
            label: "注册",
            children: (
              <Form
                form={registerForm}
                layout="vertical"
                onFinish={handleRegister}
                autoComplete="off"
              >
                <Form.Item
                  name="mail"
                  label="邮箱"
                  rules={[
                    { required: true, message: "请输入邮箱" },
                    { type: "email", message: "邮箱格式不正确" },
                  ]}
                >
                  <Input prefix={<MailOutlined />} placeholder="you@example.com" />
                </Form.Item>
                <Form.Item
                  name="code"
                  label="验证码"
                  rules={[{ required: true, message: "请输入验证码" }]}
                >
                  <Input
                    prefix={<SafetyOutlined />}
                    placeholder="6 位邮箱验证码"
                    suffix={
                      <Button
                        type="link"
                        size="small"
                        loading={codeLoading}
                        disabled={codeCountdown > 0}
                        onClick={handleSendCode}
                      >
                        {codeCountdown > 0 ? `${codeCountdown}s` : "发送验证码"}
                      </Button>
                    }
                  />
                </Form.Item>
                <Form.Item
                  name="password"
                  label="密码"
                  rules={[
                    { required: true, message: "请输入密码" },
                    { min: 6, message: "密码至少 6 位" },
                  ]}
                >
                  <Input.Password prefix={<LockOutlined />} placeholder="密码" />
                </Form.Item>
                <Form.Item
                  name="rePassword"
                  label="确认密码"
                  rules={[{ required: true, message: "请再次输入密码" }]}
                >
                  <Input.Password prefix={<LockOutlined />} placeholder="再次输入" />
                </Form.Item>
                <Button
                  type="primary"
                  htmlType="submit"
                  block
                  loading={registerLoading}
                >
                  注册并登录
                </Button>
              </Form>
            ),
          },
        ]}
      />
    </Modal>
  );
};

export default LoginRegisterModal;
