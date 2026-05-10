import React, { useEffect, useState } from "react";
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Select } from "antd";
import {
  type ModelVO,
  type UpdateModelRequest,
  EnumType,
} from "../../api/api.ts";
import { useEnumOptions } from "../../hooks/useEnumOptions";

interface EditModelModalProps {
  open: boolean;
  model: ModelVO | null;
  onCancel: () => void;
  onUpdated: () => void;
  onDeleted: () => void;
  onUpdate: (modelId: number, body: UpdateModelRequest) => Promise<void>;
  onDelete: (modelId: number) => Promise<void>;
}

const EditModelModal: React.FC<EditModelModalProps> = ({
  open,
  model,
  onCancel,
  onUpdated,
  onDeleted,
  onUpdate,
  onDelete,
}) => {
  const [form] = Form.useForm<UpdateModelRequest & { apiKey?: string }>();
  const [submitting, setSubmitting] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const {
    options: providers,
    loading: providersLoading,
    reload: reloadProviders,
  } = useEnumOptions(EnumType.ModelProviderType, open);

  useEffect(() => {
    if (open && model) {
      form.setFieldsValue({
        modelName: model.modelName,
        providerType: model.providerType,
        baseUrl: model.baseUrl,
        apiKey: model.apiKey,
        maxTokens: model.maxTokens,
      });
    } else if (!open) {
      form.resetFields();
    }
  }, [open, model, form]);

  const handleOk = async () => {
    if (!model) return;
    try {
      const values = await form.validateFields();
      const body: UpdateModelRequest = { ...values };
      if (!body.apiKey) delete body.apiKey;
      setSubmitting(true);
      await onUpdate(model.id, body);
      onUpdated();
    } catch {
      // validation or network error, antd handles toast
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!model) return;
    setDeleting(true);
    try {
      await onDelete(model.id);
      onDeleted();
    } finally {
      setDeleting(false);
    }
  };

  return (
    <Modal
      open={open}
      title="编辑模型"
      onCancel={onCancel}
      destroyOnHidden
      width={480}
      footer={
        <div className="flex items-center justify-between">
          <Popconfirm
            title="确定要删除这个模型吗？"
            description="删除后将无法恢复，已关联该模型的智能体将受到影响"
            onConfirm={handleDelete}
            okText="确定删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
          >
            <Button danger loading={deleting}>
              删除
            </Button>
          </Popconfirm>
          <div className="flex gap-2">
            <Button onClick={onCancel}>取消</Button>
            <Button type="primary" loading={submitting} onClick={handleOk}>
              确认
            </Button>
          </div>
        </div>
      }
    >
      <Form form={form} layout="vertical" autoComplete="off" className="mt-2">
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
        <Form.Item name="apiKey" label="API Key">
          <Input.Password placeholder="不修改请留空" />
        </Form.Item>
        <Form.Item name="maxTokens" label="最大 Token 数（可选）">
          <InputNumber min={1} max={1000000} className="w-full" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default EditModelModal;
