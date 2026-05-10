import React, { useState } from "react";
import {
  Alert,
  Button,
  Input,
  Modal,
  Radio,
  Space,
  Typography,
  message as antdMessage,
} from "antd";
import { CopyOutlined } from "@ant-design/icons";
import {
  buildShareUrl,
  createShareLink,
  type ShareLinkVO,
} from "../../api/share";

interface ShareLinkModalProps {
  open: boolean;
  sessionId: string | null;
  onClose: () => void;
}

const EXPIRE_OPTIONS: { label: string; value: number | null }[] = [
  { label: "永不过期", value: null },
  { label: "7 天", value: 7 },
  { label: "30 天", value: 30 },
];

const ShareLinkModal: React.FC<ShareLinkModalProps> = ({ open, sessionId, onClose }) => {
  const [submitting, setSubmitting] = useState(false);
  const [expireDays, setExpireDays] = useState<number | null>(null);
  const [link, setLink] = useState<ShareLinkVO | null>(null);

  const reset = () => {
    setLink(null);
    setExpireDays(null);
  };

  const handleCreate = async () => {
    if (!sessionId) return;
    setSubmitting(true);
    try {
      const vo = await createShareLink({
        sessionId,
        expireDays: expireDays ?? undefined,
      });
      setLink(vo);
      antdMessage.success("分享链接已生成");
    } finally {
      setSubmitting(false);
    }
  };

  const url = link ? buildShareUrl(link.slug) : "";

  return (
    <Modal
      open={open}
      title="分享会话"
      onCancel={() => {
        reset();
        onClose();
      }}
      footer={null}
      destroyOnHidden
    >
      {!sessionId ? (
        <Alert type="warning" message="当前没有可分享的会话" showIcon />
      ) : !link ? (
        <Space direction="vertical" className="w-full" size="middle">
          <div>
            <Typography.Text type="secondary">
              将创建该会话的只读快照公开链接，访问者无需登录。
            </Typography.Text>
          </div>
          <div>
            <div className="text-sm mb-2">链接有效期</div>
            <Radio.Group
              value={expireDays}
              onChange={(e) => setExpireDays(e.target.value)}
              optionType="button"
              buttonStyle="solid"
              options={EXPIRE_OPTIONS}
            />
          </div>
          <div className="flex justify-end">
            <Button type="primary" loading={submitting} onClick={handleCreate}>
              生成分享链接
            </Button>
          </div>
        </Space>
      ) : (
        <Space direction="vertical" className="w-full" size="middle">
          <Alert
            type="success"
            showIcon
            message="链接已生成，复制后发给好友访问"
          />
          <Input
            readOnly
            value={url}
            addonAfter={
              <CopyOutlined
                onClick={() => {
                  navigator.clipboard
                    .writeText(url)
                    .then(() => antdMessage.success("已复制"))
                    .catch(() => antdMessage.error("复制失败"));
                }}
                className="cursor-pointer"
              />
            }
          />
          <div className="flex justify-between">
            <Button onClick={reset}>再生成一个</Button>
            <Button
              type="primary"
              onClick={() => {
                reset();
                onClose();
              }}
            >
              完成
            </Button>
          </div>
        </Space>
      )}
    </Modal>
  );
};

export default ShareLinkModal;
