import React, { useEffect, useState } from "react";
import { Form, Input, InputNumber, Modal, Select } from "antd";
import { type CreateModelRequest, EnumType } from "../../api/api.ts";
import { useEnumOptions } from "../../hooks/useEnumOptions";

interface CustomModelModalProps {
  open: boolean;
  onCancel: () => void;
  /** 提交成功后回调新模型 id，让父组件自动选中 */
  onCreated: (modelId: number) => void;
  /** 实际创建模型的 handle，由父组件传入（用 useModels 复用 state） */
  onSubmit: (body: CreateModelRequest) => Promise<number>;
}

const CustomModelModal: React.FC<CustomModelModalProps> = ({
  open,
  onCancel,
  onCreated,
  onSubmit,
}) => {
  const [form] = Form.useForm<CreateModelRequest>();
  const [submitting, setSubmitting] = useState(false);
  // 仅在 Modal 打开后启用；Select 下拉打开时额外强制刷新一次，保证能拿到最新的枚举值
  const {
    options: providers,
    loading: providersLoading,
    reload: reloadProviders,
  } = useEnumOptions(EnumType.ModelProviderType, open);

  // 关闭时重置表单
  useEffect(() => {
    if (!open) form.resetFields();
  }, [open, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const id = await onSubmit(values);
      onCreated(id);
    } catch (e) {
      // 校验或网络错误，由 antd / http.ts 自行提示
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      title="自定义模型"
      onCancel={onCancel}
      onOk={handleOk}
      okText="确定"
      cancelText="取消"
      confirmLoading={submitting}
      maskClosable
      destroyOnHidden
      width={480}
    >
      <Form
        form={form}
        layout="vertical"
        autoComplete="off"
        initialValues={{ maxTokens: 4096 }}
      >
        <Form.Item
          name="modelName"
          label="模型名称"
          rules={[{ required: true, message: "请输入模型名称" }]}
        >
          <Input placeholder="例如 deepseek-chat / glm-4.6 / gpt-4o" />
        </Form.Item>
        <Form.Item
          name="providerType"
          label="模型厂商"
          rules={[{ required: true, message: "请选择模型厂商" }]}
        >
          <Select
            placeholder="选择厂商"
            loading={providersLoading}
            options={providers.map((code) => ({ value: code, label: code }))}
            onDropdownVisibleChange={(visible) => {
              if (visible) reloadProviders(true);
            }}
          />
        </Form.Item>
        <Form.Item
          name="baseUrl"
          label="Base URL"
          rules={[{ required: true, message: "请输入 Base URL" }]}
        >
          <Input placeholder="例如 https://api.deepseek.com/v1" />
        </Form.Item>
        <Form.Item
          name="apiKey"
          label="API Key"
          rules={[{ required: true, message: "请输入 API Key" }]}
        >
          <Input.Password placeholder="sk-..." />
        </Form.Item>
        <Form.Item name="maxTokens" label="最大 Token 数（可选）">
          <InputNumber min={1} max={1000000} className="w-full" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CustomModelModal;
