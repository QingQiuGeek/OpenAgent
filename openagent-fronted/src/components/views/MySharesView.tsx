import React, { useEffect, useState } from "react";
import {
  Button,
  Empty,
  message as antdMessage,
  Popconfirm,
  Space,
  Table,
  Tag,
  Typography,
} from "antd";
import type { TableColumnsType } from "antd";
import { ArrowLeftOutlined, CopyOutlined, LinkOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import {
  buildShareUrl,
  getMyShareLinks,
  revokeShareLink,
  type ShareLinkVO,
} from "../../api/share";

const MySharesView: React.FC = () => {
  const navigate = useNavigate();
  const [list, setList] = useState<ShareLinkVO[]>([]);
  const [loading, setLoading] = useState(false);

  const reload = () => {
    setLoading(true);
    getMyShareLinks()
      .then(setList)
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    reload();
  }, []);

  const handleCopy = (slug: string) => {
    const url = buildShareUrl(slug);
    navigator.clipboard
      .writeText(url)
      .then(() => antdMessage.success("已复制"))
      .catch(() => antdMessage.error("复制失败"));
  };

  const handleRevoke = async (id: string) => {
    await revokeShareLink(id);
    antdMessage.success("已撤销");
    reload();
  };

  const columns: TableColumnsType<ShareLinkVO> = [
    {
      title: "Slug",
      dataIndex: "slug",
      key: "slug",
      width: 180,
      render: (slug: string) => (
        <Typography.Text copyable={{ text: buildShareUrl(slug) }}>
          /share/{slug}
        </Typography.Text>
      ),
    },
    { title: "会话 ID", dataIndex: "sessionId", key: "sessionId", ellipsis: true },
    {
      title: "过期时间",
      dataIndex: "expireAt",
      key: "expireAt",
      width: 180,
      render: (v?: string) =>
        v ? (
          new Date(v).toLocaleString()
        ) : (
          <Tag color="green">永不过期</Tag>
        ),
    },
    {
      title: "浏览数",
      dataIndex: "viewCount",
      key: "viewCount",
      width: 90,
    },
    {
      title: "创建时间",
      dataIndex: "createdAt",
      key: "createdAt",
      width: 180,
      render: (v?: string) => (v ? new Date(v).toLocaleString() : "-"),
    },
    {
      title: "操作",
      key: "actions",
      width: 280,
      fixed: "right",
      render: (_, r) => (
        <Space size="small">
          <Button
            type="link"
            icon={<LinkOutlined />}
            onClick={() => window.open(buildShareUrl(r.slug), "_blank", "noopener,noreferrer")}
          >
            打开
          </Button>
          <Button type="link" icon={<CopyOutlined />} onClick={() => handleCopy(r.slug)}>
            复制
          </Button>
          <Popconfirm title="撤销该分享？" onConfirm={() => handleRevoke(r.id)}>
            <Button type="link" danger>
              撤销
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
        <Typography.Title level={3} className="!mb-0">
          我的分享
        </Typography.Title>
      </div>
      {list.length === 0 && !loading ? (
        <Empty description="还没有分享链接" />
      ) : (
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={list}
          pagination={{ pageSize: 10 }}
          scroll={{ x: 1100 }}
        />
      )}
    </div>
  );
};

export default MySharesView;
