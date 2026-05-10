import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  Card,
  Col,
  DatePicker,
  Empty,
  List,
  Popconfirm,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Tooltip,
  Typography,
  message as antdMessage,
} from "antd";
import type { TableColumnsType } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import {
  ArrowLeftOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
} from "@ant-design/icons";
import {
  Cell,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip as RTooltip,
  XAxis,
  YAxis,
} from "recharts";
import { useNavigate } from "react-router-dom";
import {
  getUsageOverview,
  getUsageDaily,
  getUsageByModel,
  getUsageList,
  type UsageOverviewVO,
  type DailyUsageVO,
  type ModelUsageVO,
  type AgentUsageLogVO,
  type PageResult,
} from "../../api/adminUsage";
import { useModels } from "../../hooks/useModels";
import type { ModelVO } from "../../api/api";
import CustomModelModal from "../modals/CustomModelModal";
import EditModelModal from "../modals/EditModelModal";

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

const PIE_COLORS = [
  "#1677ff",
  "#52c41a",
  "#faad14",
  "#eb2f96",
  "#722ed1",
  "#13c2c2",
  "#fa541c",
  "#a0d911",
];

const fmt = (d?: Dayjs | null) => (d ? d.format("YYYY-MM-DD") : undefined);

const AdminUsageDashboardView: React.FC = () => {
  const navigate = useNavigate();
  const [range, setRange] = useState<[Dayjs, Dayjs]>(() => [
    dayjs().subtract(6, "day"),
    dayjs(),
  ]);

  const params = useMemo(
    () => ({ from: fmt(range[0]), to: fmt(range[1]) }),
    [range],
  );

  const [overview, setOverview] = useState<UsageOverviewVO | null>(null);
  const [daily, setDaily] = useState<DailyUsageVO[]>([]);
  const [byModel, setByModel] = useState<ModelUsageVO[]>([]);
  const [logs, setLogs] = useState<PageResult<AgentUsageLogVO> | null>(null);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [loading, setLoading] = useState(false);

  const [customOpen, setCustomOpen] = useState(false);
  const [editingModel, setEditingModel] = useState<ModelVO | null>(null);

  const {
    models,
    loading: modelsLoading,
    createModelHandle,
    updateModelHandle,
    deleteModelHandle,
    refresh: refreshModels,
  } = useModels();

  const modelIdToName = useMemo(() => {
    const m = new Map<number | string, string>();
    models.forEach((it) => m.set(it.id, it.modelName));
    byModel.forEach((it) => {
      if (it.modelId != null) m.set(it.modelId as unknown as number, it.modelName);
    });
    return m;
  }, [models, byModel]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([
      getUsageOverview(params),
      getUsageDaily(params),
      getUsageByModel(params),
      getUsageList({ ...params, page, pageSize }),
    ])
      .then(([ov, dl, bm, lg]) => {
        if (cancelled) return;
        setOverview(ov);
        setDaily(dl);
        setByModel(bm);
        setLogs(lg);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [params, page, pageSize]);

  /* ---------------- 调用明细列（响应式：窄屏隐藏次要列） ---------------- */
  const logColumns: TableColumnsType<AgentUsageLogVO> = [
    {
      title: "时间",
      dataIndex: "createdAt",
      key: "createdAt",
      width: 170,
      render: (v: string | null | undefined) =>
        v ? dayjs(v).format("YYYY-MM-DD HH:mm:ss") : "-",
    },
    {
      title: "模型",
      dataIndex: "modelId",
      key: "modelId",
      width: 180,
      render: (v) => {
        const name = modelIdToName.get(v) ?? `#${v}`;
        return <Tag color="blue">{name}</Tag>;
      },
    },
    {
      title: (
        <Tooltip title="发起对话时的模式：普通=直接对话；联网搜索=允许调用 web_search 工具">
          模式
        </Tooltip>
      ),
      dataIndex: "chatMode",
      key: "chatMode",
      width: 100,
      responsive: ["md"],
      render: (v?: string) => {
        const map: Record<string, { color: string; label: string }> = {
          normal: { color: "default", label: "普通" },
          web_search: { color: "geekblue", label: "联网搜索" },
        };
        const it = map[v || "normal"] || { color: "default", label: v || "-" };
        return <Tag color={it.color}>{it.label}</Tag>;
      },
    },
    {
      title: (
        <Tooltip title="本次对话累计的 token（多轮 ReAct 会累加每一轮 LLM 返回的 tokenUsage）">
          Token
        </Tooltip>
      ),
      dataIndex: "totalTokens",
      key: "totalTokens",
      width: 90,
      responsive: ["sm"],
    },
    {
      title: (
        <Tooltip title="服务端从 agent 启动到 finally 块的总耗时，含工具调用与多轮 LLM round-trip">
          耗时(ms)
        </Tooltip>
      ),
      dataIndex: "latencyMs",
      key: "latencyMs",
      width: 100,
      responsive: ["md"],
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 80,
      responsive: ["lg"],
      render: (v: string) =>
        v === "error" ? (
          <Tag color="red">{v}</Tag>
        ) : (
          <Tag color="green">{v ?? "-"}</Tag>
        ),
    },
    {
      title: "错误信息",
      dataIndex: "errorMsg",
      key: "errorMsg",
      ellipsis: true,
      responsive: ["lg"],
    },
  ];

  /* ---------------- 模型列表 ---------------- */
  const handleDeleteModel = async (m: ModelVO) => {
    try {
      await deleteModelHandle(m.id);
      antdMessage.success(`已删除 ${m.modelName}`);
    } catch (e) {
      antdMessage.error(e instanceof Error ? e.message : "删除失败");
    }
  };

  /* ---------------- 饼图数据 ---------------- */
  const pieData = useMemo(
    () =>
      byModel.map((it) => ({
        name: it.modelName,
        value: it.totalTokens,
        calls: it.calls,
      })),
    [byModel],
  );

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
      <div className="flex items-center justify-between mb-4 flex-wrap gap-2">
        <Title level={3} className="!mb-0">
          我的模型
        </Title>
        <RangePicker
          value={range}
          onChange={(v) => v && v[0] && v[1] && setRange([v[0], v[1]])}
          allowClear={false}
        />
      </div>

      {/* 行1: 紧凑统计卡 + 模型列表，同一行 */}
      <Row gutter={[16, 16]} className="mb-4">
        <Col xs={24} lg={9}>
          <Card title="用量概览" loading={loading} className="h-full">
            <Row gutter={[12, 12]}>
              <Col span={12}>
                <Statistic
                  title="调用总数"
                  value={overview?.totalCalls ?? 0}
                  valueStyle={{ fontSize: 22 }}
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title="Token 总消耗"
                  value={overview?.totalTokens ?? 0}
                  valueStyle={{ fontSize: 22 }}
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title="平均时延"
                  value={overview?.avgLatencyMs ?? 0}
                  suffix="ms"
                  valueStyle={{ fontSize: 22 }}
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title="错误率"
                  value={(overview?.errorRate ?? 0) * 100}
                  precision={2}
                  suffix="%"
                  valueStyle={{
                    fontSize: 22,
                    color:
                      (overview?.errorRate ?? 0) > 0.05 ? "#cf1322" : undefined,
                  }}
                />
              </Col>
            </Row>
          </Card>
        </Col>
        <Col xs={24} lg={15}>
          <Card
            title={`我的模型 (${models.length})`}
            loading={modelsLoading}
            className="h-full"
            extra={
              <Button
                type="primary"
                size="small"
                icon={<PlusOutlined />}
                onClick={() => setCustomOpen(true)}
              >
                自定义模型
              </Button>
            }
            styles={{ body: { padding: 0 } }}
          >
            {models.length === 0 ? (
              <div className="p-6">
                <Empty description="还没有模型，点击右上角自定义一个" />
              </div>
            ) : (
              <div style={{ maxHeight: 240, overflowY: "auto" }}>
                <List
                  size="small"
                  dataSource={models}
                  renderItem={(m) => (
                    <List.Item
                      className="!px-4"
                      actions={[
                        <Button
                          key="edit"
                          type="text"
                          size="small"
                          icon={<EditOutlined />}
                          onClick={() => setEditingModel(m)}
                        />,
                        <Popconfirm
                          key="del"
                          title="确定删除该模型？"
                          onConfirm={() => handleDeleteModel(m)}
                        >
                          <Button
                            type="text"
                            size="small"
                            danger
                            icon={<DeleteOutlined />}
                          />
                        </Popconfirm>,
                      ]}
                    >
                      <Space size="small" className="min-w-0 flex-1">
                        <Text strong className="truncate" title={m.modelName}>
                          {m.modelName}
                        </Text>
                        <Tag color="blue" className="!m-0">
                          {m.providerType}
                        </Tag>
                      </Space>
                    </List.Item>
                  )}
                />
              </div>
            )}
          </Card>
        </Col>
      </Row>

      {/* 行2: 按日聚合 折线图 + 按模型聚合 饼图，同一行同等大小 */}
      <Row gutter={[16, 16]} className="mb-4">
        <Col xs={24} lg={12}>
          <Card title="按日聚合" loading={loading} className="h-full">
            {daily.length === 0 ? (
              <Empty description="暂无数据" />
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <LineChart data={daily} margin={{ left: 8, right: 16, top: 8 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis dataKey="date" fontSize={12} />
                  <YAxis yAxisId="left" fontSize={12} />
                  <YAxis yAxisId="right" orientation="right" fontSize={12} />
                  <RTooltip />
                  <Legend />
                  <Line
                    yAxisId="left"
                    type="monotone"
                    dataKey="totalTokens"
                    name="Token"
                    stroke="#1677ff"
                    strokeWidth={2}
                    dot={{ r: 3 }}
                  />
                  <Line
                    yAxisId="right"
                    type="monotone"
                    dataKey="calls"
                    name="调用数"
                    stroke="#52c41a"
                    strokeWidth={2}
                    dot={{ r: 3 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            )}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="按模型聚合（Token 占比）" loading={loading} className="h-full">
            {pieData.length === 0 ? (
              <Empty description="暂无数据" />
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <PieChart>
                  <RTooltip
                    formatter={(value, _name, item) => {
                      const calls =
                        (item as { payload?: { calls?: number } })?.payload
                          ?.calls ?? 0;
                      const label =
                        (item as { payload?: { name?: string } })?.payload
                          ?.name ?? "";
                      return [`${value} tokens / ${calls} 次`, label];
                    }}
                  />
                  <Legend />
                  <Pie
                    data={pieData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="50%"
                    outerRadius={90}
                    innerRadius={45}
                    label={(entry) => entry.name}
                  >
                    {pieData.map((_, i) => (
                      <Cell
                        key={`cell-${i}`}
                        fill={PIE_COLORS[i % PIE_COLORS.length]}
                      />
                    ))}
                  </Pie>
                </PieChart>
              </ResponsiveContainer>
            )}
          </Card>
        </Col>
      </Row>

      {/* 行3: 调用明细 */}
      <Card title="调用明细" loading={loading} className="mb-4">
        <Table
          size="small"
          rowKey="id"
          columns={logColumns}
          dataSource={logs?.records ?? []}
          scroll={{ x: "max-content" }}
          pagination={{
            current: page,
            pageSize,
            total: logs?.total ?? 0,
            onChange: (p, ps) => {
              setPage(p);
              setPageSize(ps);
            },
            showSizeChanger: true,
          }}
        />
      </Card>

      <Space className="mt-2 text-gray-400 text-xs">
        提示：仅展示当前账号的调用数据；从功能上线后开始记录，更早的对话不计入。
      </Space>

      {/* 自定义模型 / 编辑模型 弹窗 */}
      <CustomModelModal
        open={customOpen}
        onCancel={() => setCustomOpen(false)}
        onSubmit={createModelHandle}
        onCreated={() => {
          setCustomOpen(false);
          refreshModels();
        }}
      />
      <EditModelModal
        open={!!editingModel}
        model={editingModel}
        onCancel={() => setEditingModel(null)}
        onUpdate={updateModelHandle}
        onDelete={deleteModelHandle}
        onUpdated={() => setEditingModel(null)}
        onDeleted={() => setEditingModel(null)}
      />
    </div>
  );
};

export default AdminUsageDashboardView;
