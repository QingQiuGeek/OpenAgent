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
  MinusCircleOutlined,
  PlusOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import {
  createMcpServer,
  deleteMcpServer,
  listMcpServers,
  listMyMcpToolGroups,
  testMcpServerConnection,
  updateMcpServer,
  type CreateMcpServerRequest,
  type McpServerVO,
  type McpToolGroupVO,
} from "../../api/mcpServer.ts";
import { useEnumOptions } from "../../hooks/useEnumOptions.ts";
import { EnumType } from "../../api/api.ts";

interface HeaderKV {
  key?: string;
  value?: string;
}

interface FormValues extends Omit<CreateMcpServerRequest, "headers"> {
  enabledBool?: boolean;
  /** UI 内部表示，提交时序列化为 JSON 字符串塞回 headers。 */
  headersList?: HeaderKV[];
}

/** 把 JSON 字符串解析回 KV 数组；解析失败/空值返回空数组。 */
function parseHeadersJson(json?: string | null): HeaderKV[] {
  if (!json) return [];
  try {
    const obj = JSON.parse(json);
    if (obj && typeof obj === "object" && !Array.isArray(obj)) {
      return Object.entries(obj as Record<string, unknown>).map(([k, v]) => ({
        key: k,
        value: String(v ?? ""),
      }));
    }
  } catch {
    /* ignore */
  }
  return [];
}

const McpServerView: React.FC = () => {
  const navigate = useNavigate();
  const [list, setList] = useState<McpServerVO[]>([]);
  /** mcpServerId → 该 server 的工具分组。仅含启用且正常连接的 MCP。 */
  const [toolsByMcp, setToolsByMcp] = useState<Record<number, McpToolGroupVO>>(
    {},
  );
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState<McpServerVO | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [form] = Form.useForm<FormValues>();
  const {
    options: transportOptions,
    loading: transportLoading,
    reload: reloadTransports,
  } = useEnumOptions(EnumType.McpTransport, modalOpen);

  // 仅展示远程类型（sse / http / streamable_http）。stdio 需要后端容器内 fork 子进程，
  // 当前部署形态不支持，前端兜底过滤一道，防止字典漏出旧值。
  const transportSelectOptions = useMemo(
    () =>
      transportOptions
        .filter((v) => v !== "stdio")
        .map((v) => ({ label: v, value: v })),
    [transportOptions],
  );

  const reload = () => {
    setLoading(true);
    // 同时拉服务列表与已启用服务的工具列表，后者会静默含失败项（ok=false + errorMsg）。
    Promise.all([listMcpServers(), listMyMcpToolGroups()])
      .then(([servers, groups]) => {
        setList(servers);
        const map: Record<number, McpToolGroupVO> = {};
        groups.forEach((g) => {
          map[g.mcpServerId] = g;
        });
        setToolsByMcp(map);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    reload();
  }, []);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      transport: transportSelectOptions[0]?.value || "sse",
      enabledBool: true,
      headersList: [],
    });
    setModalOpen(true);
  };

  const openEdit = (row: McpServerVO) => {
    setEditing(row);
    form.setFieldsValue({
      name: row.name,
      description: row.description,
      transport: row.transport,
      url: row.url,
      enabledBool: row.enabled === 1,
      headersList: parseHeadersJson(row.headers),
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    // 序列化 headersList → JSON 字符串。校验已交给 Form.Item rules，这里到此都是合法 KV。
    let headersJson: string | undefined;
    const list = values.headersList ?? [];
    if (list.length > 0) {
      const obj: Record<string, string> = {};
      list.forEach((kv) => {
        if (kv.key) obj[kv.key.trim()] = (kv.value ?? "").trim();
      });
      headersJson = Object.keys(obj).length ? JSON.stringify(obj) : undefined;
    }
    const body: CreateMcpServerRequest = {
      name: values.name,
      description: values.description,
      transport: values.transport,
      url: values.url,
      headers: headersJson,
      enabled: values.enabledBool ? 1 : 0,
    };
    setSubmitting(true);
    try {
      let mcpServerId: number;
      if (editing) {
        await updateMcpServer(editing.id, body);
        mcpServerId = editing.id;
      } else {
        const resp = await createMcpServer(body);
        mcpServerId = resp.mcpServerId;
      }
      // 保存成功后立即试连：让用户第一时间知道配置是否可用。
      // 如果连接失败，记录已入库/已更新，不回滚，仅提醒用户去调。
      try {
        const tools = await testMcpServerConnection(mcpServerId);
        antdMessage.success(
          tools.length === 0
            ? "连接成功，但未发现可用工具"
            : `连接成功，发现 ${tools.length} 个工具：${tools.join("、")}`,
          5,
        );
      } catch {
        // http.ts 拦截层已弹出具体错误信息，这里补个提示。
        antdMessage.warning("已保存，但连接测试未通过，请检查配置");
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

  const handleToggleEnabled = async (row: McpServerVO, next: boolean) => {
    await updateMcpServer(row.id, { enabled: next ? 1 : 0 });
    reload();
  };

  const columns: TableColumnsType<McpServerVO> = [
    { title: "名称", dataIndex: "name", key: "name", width: 140, ellipsis: true, fixed: "left" },
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
      width: 220,
      ellipsis: true,
      render: (_, r) => (
        <Typography.Text
          type="secondary"
          ellipsis={{ tooltip: r.url }}
          style={{ maxWidth: 200 }}
        >
          {r.url}
        </Typography.Text>
      ),
    },
    {
      title: "工具",
      key: "tools",
      width: 320,
      render: (_, r) => {
        const g = toolsByMcp[r.id];
        if (r.enabled !== 1) {
          return <Typography.Text type="secondary">未启用</Typography.Text>;
        }
        if (!g) {
          return <Typography.Text type="secondary">—</Typography.Text>;
        }
        if (!g.ok) {
          return (
            <Tooltip title={g.errorMsg || "连接失败"}>
              <Tag color="red">连接失败</Tag>
            </Tooltip>
          );
        }
        if (!g.toolNames?.length) {
          return <Typography.Text type="secondary">无工具</Typography.Text>;
        }
        // 工具可能很多，单行展示 + 水平滚动，避免换行顶高表格。
        return (
          <div
            className="overflow-x-auto whitespace-nowrap scrollbar-thin"
            style={{ maxWidth: 300 }}
          >
            {g.toolNames.map((t) => (
              <Tag key={t} color="geekblue" className="!mr-1">
                {t}
              </Tag>
            ))}
          </div>
        );
      },
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
      width: 160,
      fixed: "right",
      render: (_, r) => (
        <Space size="small">
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
          scroll={{ x: "max-content" }}
        />
      )}

      <Modal
        title={editing ? "编辑 MCP 服务" : "新增 MCP 服务"}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        okText={submitting ? "连接 MCP 中..." : "连接"}
        cancelText="取消"
        destroyOnHidden
        width={460}
      >
        <Form
          form={form}
          layout="vertical"
          preserve={false}
          size="small"
          className="mcp-compact-form"
        >
          <Form.Item
            label="名称"
            name="name"
            rules={[{ required: true, message: "请输入名称" }]}
          >
            <Input placeholder="例如：filesystem-mcp" />
          </Form.Item>
          <Form.Item label="描述" name="description">
            <Input placeholder="可选" />
          </Form.Item>
          <div className="grid grid-cols-2 gap-x-3">
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
            <Form.Item label="启用" name="enabledBool" valuePropName="checked">
              <Switch size="small" />
            </Form.Item>
          </div>
          <Form.Item
            label="服务地址"
            name="url"
            tooltip="支持sse、http、streamhttp。如有路径参数请直接拼接到url中。如?k1=v1&k2=v2"
            rules={[{ required: true, message: "请填写 MCP 服务地址" }]}
          >
            <Input placeholder="https://example.com/mcp/sse" />
          </Form.Item>
          <Form.Item
            label="自定义请求头"
            style={{ marginBottom: 0 }}
          >
            <Form.List name="headersList">
              {(fields, { add, remove }) => (
                <>
                  {fields.map(({ key, name, ...rest }) => (
                    <Space
                      key={key}
                      align="baseline"
                      className="flex w-full mb-2"
                      size="small"
                    >
                      <Form.Item
                        {...rest}
                        name={[name, "key"]}
                        rules={[{ required: true, message: "Key 必填" }]}
                        className="!mb-0 flex-1"
                      >
                        <Input placeholder="Authorization" />
                      </Form.Item>
                      <Form.Item
                        {...rest}
                        name={[name, "value"]}
                        rules={[{ required: true, message: "Value 必填" }]}
                        className="!mb-0 flex-1"
                      >
                        <Input placeholder="Bearer xxx" />
                      </Form.Item>
                      <MinusCircleOutlined
                        onClick={() => remove(name)}
                        className="text-gray-400 hover:text-red-500"
                      />
                    </Space>
                  ))}
                  <Button
                    type="dashed"
                    onClick={() => add({ key: "", value: "" })}
                    block
                    size="small"
                    icon={<PlusOutlined />}
                  >
                    添加请求头
                  </Button>
                </>
              )}
            </Form.List>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default McpServerView;
