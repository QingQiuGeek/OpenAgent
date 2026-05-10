import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Tag, Typography } from "antd";
import { ArrowLeftOutlined, BookOutlined } from "@ant-design/icons";
import KnowledgeBaseTabContent from "../tabs/KnowledgeBaseTabContent.tsx";
import AddKnowledgeBaseModal from "../modals/AddKnowledgeBaseModal.tsx";
import KnowledgeBaseView from "./KnowledgeBaseView.tsx";
import { useKnowledgeBases } from "../../hooks/useKnowledgeBases.ts";
import { useEnumOptions } from "../../hooks/useEnumOptions.ts";
import { EnumType } from "../../api/api.ts";
import type { KnowledgeBase } from "../../types";

/**
 * 知识库列表 + 详情同页面（master-detail）。
 */
const KnowledgeBaseListView: React.FC = () => {
  const navigate = useNavigate();
  const {
    knowledgeBases,
    createKnowledgeBaseHandle,
    updateKnowledgeBaseHandle,
    deleteKnowledgeBaseHandle,
  } = useKnowledgeBases();

  const { options: fileTypes } = useEnumOptions(EnumType.DocumentFileType);

  const [selectedKbId, setSelectedKbId] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<KnowledgeBase | null>(null);

  const closeModal = () => {
    setModalOpen(false);
    setEditing(null);
  };

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
      <div className="flex items-center justify-between mb-2">
        <Typography.Title level={3} className="!mb-0 flex items-center gap-2">
          <BookOutlined />
          知识库
        </Typography.Title>
      </div>
      <Typography.Paragraph type="secondary" className="!mb-3">
        管理你的知识库；点击卡片在同页面查看 / 上传文档。
      </Typography.Paragraph>
      <div className="mb-4 flex items-center gap-2 flex-wrap">
        <span className="text-sm text-gray-500">支持文件类型：</span>
        {fileTypes.length === 0 ? (
          <span className="text-sm text-gray-400">加载中…</span>
        ) : (
          fileTypes.map((t) => (
            <Tag key={t} color="blue">
              {t}
            </Tag>
          ))
        )}
        <span className="text-xs text-gray-400">
          .md 按标题层级切段；其他文档用 Tika 抽取后弹性分段，统一向量化入库可被检索。
        </span>
      </div>

      {selectedKbId ? (
        <KnowledgeBaseView
          knowledgeBaseId={selectedKbId}
          onBack={() => setSelectedKbId(null)}
        />
      ) : (
        <KnowledgeBaseTabContent
          knowledgeBases={knowledgeBases}
          onCreateKnowledgeBaseClick={() => setModalOpen(true)}
          onSelectKnowledgeBase={(id) => setSelectedKbId(id)}
          onEditKnowledgeBase={(kb) => {
            setEditing(kb);
            setModalOpen(true);
          }}
          onDeleteKnowledgeBase={deleteKnowledgeBaseHandle}
        />
      )}

      <AddKnowledgeBaseModal
        open={modalOpen}
        onClose={closeModal}
        createKnowledgeBaseHandle={createKnowledgeBaseHandle}
        updateKnowledgeBaseHandle={updateKnowledgeBaseHandle}
        editingKnowledgeBase={editing}
      />
    </div>
  );
};

export default KnowledgeBaseListView;
