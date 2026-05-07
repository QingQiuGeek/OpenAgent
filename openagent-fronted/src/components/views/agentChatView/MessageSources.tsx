import React, { useState } from "react";
import { Tag, Tooltip } from "antd";
import {
  GlobalOutlined,
  BookOutlined,
  DownOutlined,
  RightOutlined,
} from "@ant-design/icons";
import type { SourceItem } from "../../../types";

interface MessageSourcesProps {
  sources: SourceItem[];
}

const truncate = (s: string, n: number) =>
  s && s.length > n ? s.slice(0, n) + "..." : s || "";

/**
 * 渲染 AI 消息的来源引用：
 * - web 类型：标题 + URL + 摘要
 * - kb 类型：知识库名 + 文件名 + 分块摘要
 */
const MessageSources: React.FC<MessageSourcesProps> = ({ sources }) => {
  const [expanded, setExpanded] = useState(false);

  if (!sources || sources.length === 0) return null;

  const webCount = sources.filter((s) => s.type === "web").length;
  const kbCount = sources.filter((s) => s.type === "kb").length;

  return (
    <div className="mt-2 mb-1 text-xs">
      <div
        className="inline-flex items-center gap-1.5 px-2 py-1 rounded-md bg-gray-50 hover:bg-gray-100 dark:bg-zinc-800 dark:hover:bg-zinc-700 cursor-pointer transition-colors select-none border border-gray-200 dark:border-zinc-700"
        onClick={() => setExpanded((v) => !v)}
      >
        {expanded ? (
          <DownOutlined className="!text-[10px] text-gray-400" />
        ) : (
          <RightOutlined className="!text-[10px] text-gray-400" />
        )}
        <span className="font-medium text-gray-600 dark:text-gray-300">
          来源引用
        </span>
        {webCount > 0 && (
          <Tag bordered={false} color="blue" className="!m-0 !text-[11px]">
            <GlobalOutlined /> 网络 {webCount}
          </Tag>
        )}
        {kbCount > 0 && (
          <Tag bordered={false} color="green" className="!m-0 !text-[11px]">
            <BookOutlined /> 知识库 {kbCount}
          </Tag>
        )}
      </div>

      {expanded && (
        <div className="mt-2 grid grid-cols-1 md:grid-cols-2 gap-2">
          {sources.map((src, idx) => (
            <SourceCard key={idx} source={src} index={idx + 1} />
          ))}
        </div>
      )}
    </div>
  );
};

const SourceCard: React.FC<{ source: SourceItem; index: number }> = ({
  source,
  index,
}) => {
  if (source.type === "web") {
    return (
      <a
        href={source.url}
        target="_blank"
        rel="noopener noreferrer"
        className="block p-2 rounded-md border border-gray-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 hover:border-blue-400 hover:shadow-sm transition-all no-underline"
      >
        <div className="flex items-start gap-2">
          <span className="flex-shrink-0 w-5 h-5 rounded bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-300 flex items-center justify-center text-[10px] font-semibold">
            {index}
          </span>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-1 text-blue-600 dark:text-blue-400">
              <GlobalOutlined className="!text-[11px] flex-shrink-0" />
              <Tooltip title={source.title}>
                <span className="truncate text-[12px] font-medium">
                  {source.title || source.url}
                </span>
              </Tooltip>
            </div>
            {source.url && (
              <div className="text-[10px] text-gray-400 truncate mt-0.5">
                {source.url}
              </div>
            )}
            {source.content && (
              <div className="text-[11px] text-gray-500 dark:text-gray-400 mt-1 line-clamp-2">
                {truncate(source.content, 120)}
              </div>
            )}
          </div>
        </div>
      </a>
    );
  }
  // kb
  return (
    <div className="p-2 rounded-md border border-gray-200 dark:border-zinc-700 bg-white dark:bg-zinc-900">
      <div className="flex items-start gap-2">
        <span className="flex-shrink-0 w-5 h-5 rounded bg-green-100 dark:bg-green-900 text-green-600 dark:text-green-300 flex items-center justify-center text-[10px] font-semibold">
          {index}
        </span>
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-1 text-green-700 dark:text-green-400">
            <BookOutlined className="!text-[11px] flex-shrink-0" />
            <span className="truncate text-[12px] font-medium">
              {source.title}
            </span>
          </div>
          {source.kbName && (
            <div className="text-[10px] text-gray-400 mt-0.5">
              知识库：{source.kbName}
            </div>
          )}
          {source.content && (
            <div className="text-[11px] text-gray-500 dark:text-gray-400 mt-1 line-clamp-2">
              {truncate(source.content, 120)}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default MessageSources;
