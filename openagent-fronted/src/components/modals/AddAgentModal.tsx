import React, { useEffect, useState } from "react";
import { Button, Checkbox, Divider, Input, Modal, Select, Slider, Tag, Tooltip, message } from "antd";
import TextArea from "antd/es/input/TextArea";
import { EllipsisOutlined, PlusOutlined, SaveOutlined } from "@ant-design/icons";
import {
  type CreateAgentRequest,
  type UpdateAgentRequest,
  type AgentVO,
  type ModelVO,
  getOptionalTools,
  type ToolVO,
} from "../../api/api.ts";
import { useKnowledgeBases } from "../../hooks/useKnowledgeBases.ts";
import { useModels } from "../../hooks/useModels.ts";
import CustomModelModal from "./CustomModelModal.tsx";
import EditModelModal from "./EditModelModal.tsx";

interface AddAgentModalProps {
  open: boolean;
  onClose: () => void;
  createAgentHandle: (request: CreateAgentRequest) => Promise<void>;
  updateAgentHandle?: (
    agentId: string,
    request: UpdateAgentRequest,
  ) => Promise<void>;
  editingAgent?: AgentVO | null;
}

const menuItems = [
  { key: "base", label: "基础设置" },
  { key: "model", label: "模型设置" },
  { key: "knowledge", label: "知识库设置" },
  // { key: "mcp", label: "MCP 服务器" },
  { key: "tools", label: "工具调用" },
  // { key: "memory", label: "全局记忆" },
];

const AddAgentModal: React.FC<AddAgentModalProps> = ({
  open,
  onClose,
  createAgentHandle,
  updateAgentHandle,
  editingAgent,
}) => {
  // 菜单项
  const [selectedKey, setSelectedKey] = useState<string>("base");

  // 获取知识库列表
  const { knowledgeBases, refreshKnowledgeBases } = useKnowledgeBases();

  // 打开弹窗时刷新一次，确保新建/更名后的知识库可见
  useEffect(() => {
    if (open) {
      refreshKnowledgeBases().catch(() => {});
    }
  }, [open, refreshKnowledgeBases]);

  // 模型列表
  const { models, loading: modelsLoading, createModelHandle, updateModelHandle, deleteModelHandle } = useModels();
  const [customModalOpen, setCustomModalOpen] = useState(false);
  const [editingModel, setEditingModel] = useState<ModelVO | null>(null);

  // 工具列表
  const [tools, setTools] = useState<ToolVO[]>([]);

  // 表单数据
  const [formData, setFormData] = useState<CreateAgentRequest>({
    name: "智能体助手",
    description: "",
    systemPrompt: "你是一个很有用的智能体助手",
    modelId: 0,
    allowedTools: [],
    allowedKbs: [],
    chatOptions: {
      temperature: 0.7,
      topP: 1.0,
      messageLength: 20,
    },
  });

  const [createAgentLoading, setCreateAgentLoading] = useState(false);

  // 当编辑的 agent 变化时，更新表单数据
  useEffect(() => {
    if (editingAgent) {
      setFormData({
        name: editingAgent.name,
        description: editingAgent.description || "",
        systemPrompt: editingAgent.systemPrompt || "",
        modelId: editingAgent.modelId,
        allowedTools: editingAgent.allowedTools || [],
        allowedKbs: editingAgent.allowedKbs || [],
        chatOptions: editingAgent.chatOptions || {
          temperature: 0.7,
          topP: 1.0,
          messageLength: 10,
        },
      });
    } else {
      // 重置表单
      setFormData({
        name: "agent",
        description: "",
        systemPrompt: "",
        modelId: 0,
        allowedTools: [],
        allowedKbs: [],
        chatOptions: {
          temperature: 0.7,
          topP: 1.0,
          messageLength: 10,
        },
      });
    }
  }, [editingAgent, open]);

  // 获取工具列表
  useEffect(() => {
    async function fetchTools() {
      try {
        const resp = await getOptionalTools();
        setTools(resp.tools);
      } catch (error) {
        console.error("获取工具列表失败:", error);
      }
    }

    fetchTools().then();
  }, []);

  const isEditMode = !!editingAgent;

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title={isEditMode ? "编辑智能体" : "智能体助手"}
      footer={null}
      width={800}
      centered
    >
      <div className="flex h-[500px]">
        <div className="w-[150px] h-full border-r border-gray-200 pr-2">
          <div className="flex flex-col gap-0.5 select-none cursor-pointer">
            {menuItems.map((item) => {
              const isSelected = selectedKey === item.key;
              return (
                <React.Fragment key={item.key}>
                  <div
                    onClick={() => setSelectedKey(item.key)}
                    className={`px-3 py-2 text-center rounded-lg hover:bg-gray-100 ${isSelected ? "bg-gray-100 text-gray-900 font-medium" : "text-gray-600"}`}
                  >
                    {item.label}
                  </div>
                </React.Fragment>
              );
            })}
          </div>
        </div>
        <div className="flex-1 h-full relative">
          <div className="absolute inset-0 pl-4 pr-4 pt-1 pb-16 overflow-y-auto">
            {selectedKey === "base" && (
              <div>
                <div className="mb-3">
                  <label className="block text-gray-700 font-medium mb-1">
                    名称
                  </label>
                  <div className="flex items-center">
                    <Input
                      placeholder="请输入智能体名称"
                      value={formData.name}
                      onChange={(e) =>
                        setFormData({ ...formData, name: e.target.value })
                      }
                    />
                  </div>
                </div>
                <div className="mb-3">
                  <label className="block text-gray-700 font-medium mb-1">
                    描述
                  </label>
                  <TextArea
                    placeholder="请输入智能体描述"
                    rows={2}
                    value={formData.description}
                    onChange={(e) =>
                      setFormData({ ...formData, description: e.target.value })
                    }
                  />
                </div>
                <div className="mb-3">
                  <label className="block text-gray-700 font-medium mb-1">
                    提示词
                  </label>
                  <TextArea
                    placeholder="默认提示词"
                    rows={11}
                    value={formData.systemPrompt}
                    onChange={(e) =>
                      setFormData({ ...formData, systemPrompt: e.target.value })
                    }
                  />
                </div>
              </div>
            )}
            {selectedKey === "model" && (
              <div>
                <div className="mb-4">
                  <label className="block text-gray-700 font-medium mb-1">
                    选择模型
                  </label>
                  <Select
                    options={models.map((m) => ({
                      value: m.id,
                      label: `${m.modelName}（${m.providerType}）`,
                      model: m,
                    }))}
                    optionRender={(option) => {
                      const m = (option.data as { model: ModelVO }).model;
                      return (
                        <div className="flex items-center justify-between group w-full pr-1">
                          <span className="truncate flex-1">
                            {m.modelName}
                            <span className="text-gray-400 ml-1 text-xs">
                              {m.providerType}
                            </span>
                          </span>
                            <div
                              className="invisible group-hover:visible shrink-0 ml-2 leading-none"
                              onMouseDown={(e) => {
                                e.stopPropagation();
                                e.preventDefault();
                              }}
                              onClick={(e) => {
                                e.stopPropagation();
                                e.preventDefault();
                                setEditingModel(m);
                              }}
                            >
                              <EllipsisOutlined className="text-gray-400 hover:text-blue-500 text-base" />
                            </div>
                        </div>
                      );
                    }}
                    placeholder={
                      modelsLoading ? "加载中..." : "请选择模型"
                    }
                    loading={modelsLoading}
                    style={{ width: "300px" }}
                    value={formData.modelId || undefined}
                    onChange={(value: number) =>
                      setFormData({ ...formData, modelId: value })
                    }
                    notFoundContent={
                      <div className="text-center py-2 text-gray-400 text-sm">
                        暂无模型，点击下方"自定义模型"添加
                      </div>
                    }
                    dropdownRender={(menu) => (
                      <>
                        {menu}
                        <Divider size="small"/>
                        <Button
                          type="text"
                          icon={<PlusOutlined />}
                          block
                          onMouseDown={(e) => e.preventDefault()}
                          onClick={() => setCustomModalOpen(true)}
                        >
                          自定义模型
                        </Button>
                      </>
                    )}
                  />
                </div>
                <div className="mb-4">
                  <label className="block text-gray-700 font-medium mb-2">
                    模型参数
                  </label>
                  <div className="space-y-4">
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <label className="block text-sm text-gray-600">
                          Temperature（温度）
                          <span className="text-gray-400 ml-1 text-xs">
                            (0.0 - 2.0)
                          </span>
                        </label>
                        <span className="text-sm font-medium text-gray-700 min-w-[40px] text-right">
                          {formData?.chatOptions?.temperature?.toFixed(1)}
                        </span>
                      </div>
                      <Slider
                        min={0}
                        max={2}
                        step={0.1}
                        value={formData?.chatOptions?.temperature}
                        onChange={(value) =>
                          setFormData({
                            ...formData,
                            chatOptions: {
                              ...formData.chatOptions,
                              temperature: value,
                            },
                          })
                        }
                      />
                    </div>
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <label className="block text-sm text-gray-600">
                          Top P（核采样）
                          <span className="text-gray-400 ml-1 text-xs">
                            (0.0 - 1.0)
                          </span>
                        </label>
                        <span className="text-sm font-medium text-gray-700 min-w-[40px] text-right">
                          {formData?.chatOptions?.topP?.toFixed(1)}
                        </span>
                      </div>
                      <Slider
                        min={0}
                        max={1}
                        step={0.1}
                        value={formData?.chatOptions?.topP}
                        onChange={(value) =>
                          setFormData({
                            ...formData,
                            chatOptions: {
                              ...formData.chatOptions,
                              topP: value,
                            },
                          })
                        }
                      />
                    </div>
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <label className="block text-sm text-gray-600">
                          消息窗口长度
                          <span className="text-gray-400 ml-1 text-xs">
                            (1 - 100)
                          </span>
                        </label>
                        <span className="text-sm font-medium text-gray-700 min-w-[40px] text-right">
                          {formData?.chatOptions?.messageLength}
                        </span>
                      </div>
                      <Slider
                        min={1}
                        max={100}
                        step={1}
                        value={formData?.chatOptions?.messageLength}
                        onChange={(value) =>
                          setFormData({
                            ...formData,
                            chatOptions: {
                              ...formData.chatOptions,
                              messageLength: value,
                            },
                          })
                        }
                      />
                    </div>
                  </div>
                </div>
              </div>
            )}

            {selectedKey === "knowledge" && (
              <div>
                <div className="mb-4">
                  <label className="block text-gray-700 font-medium mb-3">
                    知识库
                  </label>
                  <p className="text-sm text-gray-500 mb-4">
                    选择智能体可以访问的知识库，支持多选（最多10个）
                  </p>
                  {knowledgeBases.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">
                      <p>暂无知识库，请先创建知识库</p>
                    </div>
                  ) : (
                    <div className="space-y-3">
                      {knowledgeBases.map((kb) => {
                        const kbId = kb.knowledgeBaseId;
                        const isSelected = formData.allowedKbs?.includes(kbId);
                        return (
                          <div
                            key={kbId}
                            className={`border rounded-lg p-4 cursor-pointer transition-all hover:border-blue-400 hover:bg-blue-50 ${
                              isSelected
                                ? "border-blue-500 bg-blue-50"
                                : "border-gray-200"
                            }`}
                            onClick={() => {
                              const currentKbs = formData.allowedKbs || [];
                              if (isSelected) {
                                setFormData({
                                  ...formData,
                                  allowedKbs: currentKbs.filter(
                                    (k) => k !== kbId,
                                  ),
                                });
                              } else {
                                if (currentKbs.length >= 10) {
                                  return; // 最多选择10个
                                }
                                setFormData({
                                  ...formData,
                                  allowedKbs: [...currentKbs, kbId],
                                });
                              }
                            }}
                          >
                            <div className="flex items-start gap-2">
                              <Checkbox
                                checked={isSelected}
                                onChange={(e) => {
                                  e.stopPropagation();
                                  const currentKbs = formData.allowedKbs || [];
                                  if (e.target.checked) {
                                    if (currentKbs.length >= 10) {
                                      return; // 最多选择10个
                                    }
                                    setFormData({
                                      ...formData,
                                      allowedKbs: [...currentKbs, kbId],
                                    });
                                  } else {
                                    setFormData({
                                      ...formData,
                                      allowedKbs: currentKbs.filter(
                                        (k) => k !== kbId,
                                      ),
                                    });
                                  }
                                }}
                                className="mr-3"
                              />
                              <div className="flex-1">
                                <div className="flex items-center mb-1">
                                  <span className="font-medium text-gray-900">
                                    {kb.name}
                                  </span>
                                </div>
                                {kb.description && (
                                  <p className="text-sm text-gray-600">
                                    {kb.description}
                                  </p>
                                )}
                              </div>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
                <div>
                  <label className="block text-gray-700 font-medium mb-1">
                    检索设置
                  </label>
                </div>
              </div>
            )}
            {selectedKey === "tools" && (
              <div>
                <div className="mb-4">
                  <label className="block text-gray-700 font-medium mb-3">
                    工具调用
                  </label>
                  <p className="text-sm text-gray-500 mb-4">
                    可选工具支持多选；固定工具由系统强制启用，不可取消
                  </p>
                  {tools.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">
                      <p>暂无可用工具</p>
                    </div>
                  ) : (
                    <div className="space-y-3">
                      {[...tools]
                        .sort((a, b) => {
                          // OPTIONAL 在前，FIXED 在后
                          if (a.type === b.type) return 0;
                          return a.type === "FIXED" ? 1 : -1;
                        })
                        .map((tool) => {
                          const toolId = tool.name;
                          const isFixed = tool.type === "FIXED";
                          const isSelected = isFixed
                            ? true
                            : !!formData.allowedTools?.includes(toolId);
                          const cardClass = isFixed
                            ? "border rounded-lg p-4 border-gray-200 bg-gray-50 opacity-70 cursor-not-allowed"
                            : `border rounded-lg p-4 cursor-pointer transition-all hover:border-blue-400 hover:bg-blue-50 ${
                                isSelected
                                  ? "border-blue-500 bg-blue-50"
                                  : "border-gray-200"
                              }`;
                          return (
                            <div
                              key={toolId}
                              className={cardClass}
                              onClick={() => {
                                if (isFixed) return;
                                const currentTools =
                                  formData.allowedTools || [];
                                if (isSelected) {
                                  setFormData({
                                    ...formData,
                                    allowedTools: currentTools.filter(
                                      (t) => t !== toolId,
                                    ),
                                  });
                                } else {
                                  setFormData({
                                    ...formData,
                                    allowedTools: [...currentTools, toolId],
                                  });
                                }
                              }}
                            >
                              <div className="flex items-start gap-2">
                                <Checkbox
                                  checked={isSelected}
                                  disabled={isFixed}
                                  onChange={(e) => {
                                    if (isFixed) return;
                                    e.stopPropagation();
                                    const currentTools =
                                      formData.allowedTools || [];
                                    if (e.target.checked) {
                                      setFormData({
                                        ...formData,
                                        allowedTools: [
                                          ...currentTools,
                                          toolId,
                                        ],
                                      });
                                    } else {
                                      setFormData({
                                        ...formData,
                                        allowedTools: currentTools.filter(
                                          (t) => t !== toolId,
                                        ),
                                      });
                                    }
                                  }}
                                  className="mr-3"
                                />
                                <div className="flex-1">
                                  <div className="flex items-center gap-2 mb-1">
                                    <span
                                      className={`font-medium ${
                                        isFixed
                                          ? "text-gray-500"
                                          : "text-gray-900"
                                      }`}
                                    >
                                      {tool.name}
                                    </span>
                                    {isFixed && (
                                      <Tag
                                        color="default"
                                        className="!m-0 !text-[10px] !px-1.5 !py-0 !leading-4"
                                      >
                                        固定
                                      </Tag>
                                    )}
                                  </div>
                                  <p
                                    className={`text-sm ${
                                      isFixed
                                        ? "text-gray-400"
                                        : "text-gray-600"
                                    }`}
                                  >
                                    {tool.description}
                                  </p>
                                </div>
                              </div>
                            </div>
                          );
                        })}
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
          <div className="absolute bottom-2 right-3 z-10 bg-white rounded-md shadow-sm">
            <Button
              type="primary"
              icon={<SaveOutlined />}
              loading={createAgentLoading}
              onClick={async () => {
                if (!formData.modelId) {
                  message.warning("请先在「模型设置」中选择模型");
                  setSelectedKey("model");
                  return;
                }
                setCreateAgentLoading(true);
                try {
                  if (isEditMode && editingAgent && updateAgentHandle) {
                    await updateAgentHandle(editingAgent.id, formData);
                  } else {
                    await createAgentHandle(formData);
                  }
                  onClose();
                } finally {
                  setCreateAgentLoading(false);
                }
              }}
            >
              {isEditMode ? "更新" : "保存"}
            </Button>
          </div>
        </div>
      </div>
      <CustomModelModal
        open={customModalOpen}
        onCancel={() => setCustomModalOpen(false)}
        onSubmit={createModelHandle}
        onCreated={(modelId) => {
          setFormData((prev) => ({ ...prev, modelId }));
          setCustomModalOpen(false);
          message.success("模型已创建并选中");
        }}
      />
      <EditModelModal
        open={editingModel !== null}
        model={editingModel}
        onCancel={() => setEditingModel(null)}
        onUpdate={updateModelHandle}
        onDelete={deleteModelHandle}
        onUpdated={() => {
          setEditingModel(null);
          message.success("模型已更新");
        }}
        onDeleted={() => {
          if (editingModel && formData.modelId === editingModel.id) {
            setFormData((prev) => ({ ...prev, modelId: 0 }));
          }
          setEditingModel(null);
          message.success("模型已删除");
        }}
      />
    </Modal>
  );
};

export default AddAgentModal;
