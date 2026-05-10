import React, { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import {
  Card,
  Typography,
  Button,
  Upload,
  Table,
  Popconfirm,
  Space,
  message,
  Empty,
  Tag,
  Tooltip,
} from "antd";
import {
  BookOutlined,
  UploadOutlined,
  DeleteOutlined,
  FileOutlined,
  ArrowLeftOutlined,
  LoadingOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  MinusCircleOutlined,
} from "@ant-design/icons";
import type { UploadProps } from "antd";
import { useKnowledgeBases } from "../../hooks/useKnowledgeBases.ts";
import { useDocuments } from "../../hooks/useDocuments.ts";
import { uploadDocument, type DocumentVO, EnumType } from "../../api/api.ts";
import { useEnumOptions } from "../../hooks/useEnumOptions.ts";

const { Title, Text, Paragraph } = Typography;

interface KnowledgeBaseViewProps {
  /** 外部传入的 kb id，优先级高于 useParams（用于嵌入列表页） */
  knowledgeBaseId?: string;
  /** 提供时在顶部渲染“返回”按钮。 */
  onBack?: () => void;
}

const KnowledgeBaseView: React.FC<KnowledgeBaseViewProps> = ({
  knowledgeBaseId: kbIdProp,
  onBack,
}) => {
  const params = useParams<{ knowledgeBaseId?: string }>();
  const knowledgeBaseId = kbIdProp ?? params.knowledgeBaseId;
  const { knowledgeBases, loaded, refreshKnowledgeBases } =
    useKnowledgeBases();
  const { documents, loading, refreshDocuments, deleteDocument } =
    useDocuments(knowledgeBaseId);

  const [uploading, setUploading] = useState(false);

  // 从 enum_config 动态拉取支持的文件扩展名
  const { options: fileTypes } = useEnumOptions(EnumType.DocumentFileType);
  const KB_ACCEPT_STR = useMemo(
    () => fileTypes.map((t) => `.${t.toLowerCase()}`).join(","),
    [fileTypes],
  );

  // 查找当前知识库的详细信息
  const currentKnowledgeBase = useMemo(() => {
    if (!knowledgeBaseId) return null;
    return (
      knowledgeBases.find((kb) => kb.knowledgeBaseId === knowledgeBaseId) ||
      null
    );
  }, [knowledgeBaseId, knowledgeBases]);

  // 切换到新知识库时，如果当前列表里找不到（可能是其他实例新建的），主动刷新一次
  useEffect(() => {
    if (!knowledgeBaseId) return;
    if (loaded && !currentKnowledgeBase) {
      refreshKnowledgeBases().catch(() => {});
    }
  }, [knowledgeBaseId, loaded, currentKnowledgeBase, refreshKnowledgeBases]);

  // 状态轮询：当存在 uploading / vectorizing 的文档时每 2s 拉取一次
  const hasPending = useMemo(() => {
    return documents.some((d) => {
      const s = (d.status || "done").toLowerCase();
      return s === "uploading" || s === "vectorizing";
    });
  }, [documents]);

  useEffect(() => {
    if (!knowledgeBaseId || !hasPending) return;
    const t = setInterval(() => {
      refreshDocuments().catch(() => {});
    }, 2000);
    return () => clearInterval(t);
  }, [knowledgeBaseId, hasPending, refreshDocuments]);

  // 处理文件上传
  const handleUpload: UploadProps["customRequest"] = async (options) => {
    const { file, onSuccess, onError } = options;

    if (!knowledgeBaseId) {
      message.error("请先选择知识库");
      return;
    }

    setUploading(true);

    try {
      await uploadDocument(knowledgeBaseId, file as File);
      message.success("文档上传成功");
      await refreshDocuments();
      onSuccess?.(file);
    } catch (error) {
      message.error(error instanceof Error ? error.message : "上传失败");
      onError?.(error as Error);
    } finally {
      setUploading(false);
    }
  };

  // 格式化文件大小
  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return "0 B";
    const k = 1024;
    const sizes = ["B", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + " " + sizes[i];
  };

  // 表格列定义
  const columns = [
    {
      title: "文件名",
      dataIndex: "filename",
      key: "filename",
      render: (text: string) => (
        <Space>
          <FileOutlined />
          <span>{text}</span>
        </Space>
      ),
    },
    {
      title: "类型",
      dataIndex: "filetype",
      key: "filetype",
      width: 100,
    },
    {
      title: "大小",
      dataIndex: "size",
      key: "size",
      width: 110,
      render: (size: number) => formatFileSize(size),
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 130,
      render: (status: string | undefined, record: DocumentVO) => {
        const s = (status || "done").toLowerCase();
        if (s === "uploading") {
          return (
            <Tag icon={<LoadingOutlined />} color="processing">
              上传中
            </Tag>
          );
        }
        if (s === "vectorizing") {
          return (
            <Tag icon={<SyncOutlined spin />} color="orange">
              向量化中
            </Tag>
          );
        }
        if (s === "failed") {
          return (
            <Tooltip title={record.errorMsg || "处理失败"}>
              <Tag icon={<CloseCircleOutlined />} color="error">
                失败
              </Tag>
            </Tooltip>
          );
        }
        if (s === "skipped") {
          return (
            <Tooltip title="非 Markdown，未入向量库">
              <Tag icon={<MinusCircleOutlined />} color="default">
                已跳过
              </Tag>
            </Tooltip>
          );
        }
        return (
          <Tag icon={<CheckCircleOutlined />} color="success">
            已完成
          </Tag>
        );
      },
    },
    {
      title: "操作",
      key: "action",
      width: 100,
      render: (_: unknown, record: DocumentVO) => (
        <Popconfirm
          title="确定要删除这个文档吗？"
          description="删除后将无法恢复"
          onConfirm={() => deleteDocument(record.id)}
          okText="确定"
          cancelText="取消"
        >
          <Button type="text" danger icon={<DeleteOutlined />} size="small">
            删除
          </Button>
        </Popconfirm>
      ),
    },
  ];

  // 未选择知识库时的提示
  if (!knowledgeBaseId) {
    return (
      <div className="flex flex-col h-full items-center justify-center p-6">
        <Empty
          image={<BookOutlined className="text-6xl text-gray-300" />}
          description={
            <div className="mt-4">
              <Title level={4} type="secondary">
                未选择知识库
              </Title>
              <Text type="secondary" className="text-sm">
                请从左侧知识库列表中选择一个知识库查看详情
              </Text>
            </div>
          }
        />
      </div>
    );
  }

  // 列表尚未加载完成时显示加载态，避免误判为「不存在」
  if (!loaded) {
    return (
      <div className="flex flex-col h-full items-center justify-center p-6">
        <Text type="secondary">加载中...</Text>
      </div>
    );
  }

  // 知识库不存在
  if (!currentKnowledgeBase) {
    return (
      <div className="flex flex-col h-full items-center justify-center p-6">
        <Empty
          description={
            <div className="mt-4">
              <Title level={4} type="secondary">
                知识库不存在
              </Title>
              <Text type="secondary" className="text-sm">
                请检查知识库 ID 是否正确
              </Text>
            </div>
          }
        />
      </div>
    );
  }

  // 显示知识库详情和文档列表
  return (
    <div className={onBack ? "flex flex-col" : "flex flex-col h-full p-6 overflow-y-auto"}>
      <div className={onBack ? "w-full" : "max-w-6xl w-full mx-auto"}>
        {onBack && (
          <div className="mb-3">
            <Button icon={<ArrowLeftOutlined />} onClick={onBack}>
              返回知识库列表
            </Button>
          </div>
        )}
        <div className="mb-3">
          <Card>
            <div className="flex items-start gap-4">
              <div className="w-16 h-16 rounded-lg bg-gradient-to-br from-blue-200 to-purple-200 flex items-center justify-center text-3xl shrink-0">
                <BookOutlined />
              </div>
              <div className="flex-1">
                <Title level={3} className="mb-2">
                  {currentKnowledgeBase.name}
                </Title>
                {currentKnowledgeBase.description && (
                  <Paragraph className="text-gray-600 mb-0">
                    {currentKnowledgeBase.description}
                  </Paragraph>
                )}
                <Text type="secondary" className="text-sm">
                  知识库 ID: {currentKnowledgeBase.knowledgeBaseId}
                </Text>
              </div>
            </div>
          </Card>
        </div>
        {/* 知识库信息卡片 */}

        <div className="mb-3">
          {/* 上传 + 文档列表 合并卡片 */}
          <Card
            title={`文档列表 (${documents.length})`}
            extra={
              <Upload
                customRequest={handleUpload}
                showUploadList={false}
                accept={KB_ACCEPT_STR}
                disabled={uploading}
              >
                <Button
                  type="primary"
                  icon={<UploadOutlined />}
                  loading={uploading}
                >
                  上传文档
                </Button>
              </Upload>
            }
          >
            <Text type="secondary" className="block mb-3 text-xs">
              所有支持的类型都会被向量化入库。.md 按标题切段，其他文档用 Tika 抽取后做弹性分段。
            </Text>
            {loading ? (
              <div className="text-center py-8">
                <Text type="secondary">加载中...</Text>
              </div>
            ) : documents.length === 0 ? (
              <Empty
                description={<Text type="secondary">暂无文档，请上传文档</Text>}
              />
            ) : (
              <Table
                columns={columns}
                dataSource={documents}
                rowKey="id"
                pagination={{
                  pageSize: 10,
                  showTotal: (total) => `共 ${total} 条`,
                }}
              />
            )}
          </Card>
        </div>
      </div>
    </div>
  );
};

export default KnowledgeBaseView;
