import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  Empty,
  Form,
  Input,
  message as antdMessage,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
} from "antd";
import type { TableColumnsType } from "antd";
import {
  ApiOutlined,
  ArrowLeftOutlined,
  EditOutlined,
  DeleteOutlined,
  PlusOutlined,
  ThunderboltOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import {
  createMcpServer,
  deleteMcpServer,
  listMcpServers,
  testMcpServerConnection,
  updateMcpServer,
  type CreateMcpServerRequest,
  type McpServerVO,
} from "../../api/mcpServer.ts";
import { useEnumOptions } from "../../hooks/useEnumOptions.ts";
import { EnumType } from "../../api/api.ts";

interface FormValues extends CreateMcpServerRequest {
  enabledBool?: boolean;
}

const McpServerView: React.FC = () => {
  const navigate = useNavigate();
  const [list, setList] = useState<McpServerVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState<McpServerVO | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [testingId, setTestingId] = useState<number | null>(null);

  const [form] = Form.useForm<FormValues>();
  const {
    options: transportOptions,
    loading: transportLoading,
    reload: reloadTransports,
  } = useEnumOptions(EnumType.McpTransport, modalOpen);

  const transportSelectOptions = useMemo(
    () => transportOptions.map((v) => ({ label: v, value: v })),
    [transportOptions],
  );

  const reload = () => {
    setLoading(true);
    listMcpServers()
      .then(setList)
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    reload();
  }, []);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      transport: transportOptions[0] || "stdio",
      enabledBool: true,
    });
    setModalOpen(true);
  };

  const openEdit = (row: McpServerVO) => {
    setEditing(row);
    form.setFieldsValue({
      ...row,
      enabledBool: row.enabled === 1,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    const body: CreateMcpServerRequest = {
      name: values.name,
      description: values.description,
      transport: values.transport,
      command: values.command,
      url: values.url,
      env: values.env,
      headers: values.headers,
      enabled: values.enabledBool ? 1 : 0,
    };
    setSubmitting(true);
    try {
      if (editing) {
        await updateMcpServer(editing.id, body);
        antdMessage.success("已更新");
      } else {
        await createMcpServer(body);
        antdMessage.success("已创建");
      }
      setModalOpen(false);
      reload();
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    await deleteMcpServer(id);
    antdMessage.success("已删除");
    reload();
  };

  const handleTest = async (row: McpServerVO) => {
    setTestingId(row.id);
    try {
      const tools = await testMcpServerConnection(row.id);
      Modal.success({
        title: `${row.name} 连接成功`,
        content:
          tools.length === 0
            ? "未发现工具"
            : `共发现 ${tools.length} 个工具：${tools.join(", ")}`,
      });
    } finally {
      setTestingId(null);
    }
  };

  const handleToggleEnabled = async (row: McpServerVO, next: boolean) => {
    await updateMcpServer(row.id, { enabled: next ? 1 : 0 });
    reload();
  };

  const transportValue = Form.useWatch("transport", form);

  const columns: TableColumnsType<McpServerVO> = [
    { title: "名称", dataIndex: "name", key: "name", width: 180, ellipsis: true },
    {
      title: "传输",
      dataIndex: "transport",
      key: "transport",
      width: 100,
      render: (v: string) => <Tag color="blue">{v}</Tag>,
    },
    {
      title: "连接",
      key: "endpoint",
      ellipsis: true,
      render: (_, r) => (
        <Typography.Text type="secondary" ellipsis>
          {r.transport === "stdio" ? r.command : r.url}
        </Typography.Text>
      ),
    },
    {
      title: "启用",
      dataIndex: "enabled",
      key: "enabled",
      width: 80,
      render: (_, r) => (
        <Switch
          size="small"
          checked={r.enabled === 1}
          onChange={(v) => handleToggleEnabled(r, v)}
        />
      ),
    },
    {
      title: "操作",
      key: "actions",
      width: 240,
      render: (_, r) => (
        <Space size="small">
          <Tooltip title="测试连接">
            <Button
              type="link"
              icon={<ThunderboltOutlined />}
              loading={testingId === r.id}
              onClick={() => handleTest(r)}
              disabled={r.enabled !== 1}
            >
              测试
            </Button>
          </Tooltip>
          <Button type="link" icon={<EditOutlined />} onClick={() => openEdit(r)}>
            编辑
          </Button>
          <Popconfirm title="删除该 MCP 服务？" onConfirm={() => handleDelete(r.id)}>
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="p-6 overflow-auto h-full">
      <div className="flex items-center gap-3 mb-2">
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate("/")}
        >
          返回上一级
        </Button>
      </div>
      <div className="flex items-center justify-between mb-4">
        <Typography.Title level={3} className="!mb-0 flex items-center gap-2">
          <ApiOutlined />
          MCP 服务器
        </Typography.Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新增 MCP 服务
        </Button>
      </div>
      <Typography.Paragraph type="secondary">
        在 Agent 编辑器的「工具调用」分组中可勾选已启用的 MCP 工具；修改服务配置后会自动断开并重连。
      </Typography.Paragraph>
      {list.length === 0 && !loading ? (
        <Empty description="还没有 MCP 服务" />
      ) : (
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={list}
          pagination={{ pageSize: 10 }}
        />
      )}

      <Modal
        title={editing ? "编辑 MCP 服务" : "新增 MCP 服务"}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnHidden
        width={640}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item label="名称" name="name" rules={[{ required: true, message: "请输入名称" }]}>
            <Input placeholder="例如：filesystem-mcp" />
          </Form.Item>
          <Form.Item label="描述" name="description">
            <Input placeholder="可选" />
          </Form.Item>
          <Form.Item
            label="传输方式"
            name="transport"
            rules={[{ required: true, message: "请选择传输方式" }]}
          >
            <Select
              loading={transportLoading}
              options={transportSelectOptions}
              onDropdownVisibleChange={(visible) => {
                if (visible) reloadTransports(true);
              }}
            />
          </Form.Item>
          {transportValue === "stdio" ? (
            <>
              <Form.Item
                label="命令"
                name="command"
                rules={[{ required: true, message: "stdio 模式需要命令" }]}
              >
                <Input placeholder="例如：npx -y @modelcontextprotocol/server-filesystem ./" />
              </Form.Item>
              <Form.Item label="环境变量" name="env" tooltip="JSON 对象，例如 {&quot;FOO&quot;: &quot;bar&quot;}">
                <Input.TextArea rows={3} placeholder='{"FOO": "bar"}' />
              </Form.Item>
            </>
          ) : (
            <>
              <Form.Item
                label="SSE URL"
                name="url"
                rules={[{ required: true, message: "sse / http 模式需要 URL" }]}
              >
                <Input placeholder="https://example.com/mcp/sse" />
              </Form.Item>
              <Form.Item label="自定义请求头" name="headers" tooltip="JSON 对象。当前后端版本暂不下发到 transport，预留字段。">
                <Input.TextArea rows={3} placeholder='{"Authorization": "Bearer ..."}' />
              </Form.Item>
            </>
          )}
          <Form.Item label="启用" name="enabledBool" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default McpServerView;
