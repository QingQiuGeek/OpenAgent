import React, { useEffect, useRef, useState, useMemo } from "react";
import { Button, message as antdMessage } from "antd";
import { CopyOutlined, CheckOutlined } from "@ant-design/icons";
import hljs from "highlight.js";
import "highlight.js/styles/github-dark.css";
import mermaid from "mermaid";
import type { ComponentProps } from "@ant-design/x-markdown";

mermaid.initialize({
  startOnLoad: false,
  theme: "default",
  securityLevel: "loose",
  fontFamily: "inherit",
});

/**
 * 提取代码块原始文本。XMarkdown 传入的 domNode.children 里可能嵌套多层，
 * 直接从 DOM 节点读取文本。
 */
const getCodeText = (el: HTMLElement | null): string => {
  if (!el) return "";
  return el.textContent || "";
};

/**
 * 从 className 中解析出语言（<code class="language-xxx">）
 */
const getLanguage = (className?: string): string => {
  if (!className) return "";
  const match = /language-([\w-]+)/.exec(className);
  return match ? match[1] : "";
};

/**
 * Mermaid 图表渲染组件
 */
const MermaidBlock: React.FC<{ code: string }> = ({ code }) => {
  const ref = useRef<HTMLDivElement>(null);
  const [error, setError] = useState<string>("");
  const id = useMemo(
    () => `mermaid-${Math.random().toString(36).slice(2, 10)}`,
    [],
  );

  useEffect(() => {
    if (!ref.current || !code) return;
    let cancelled = false;
    setError("");
    mermaid
      .render(id, code)
      .then(({ svg }) => {
        if (!cancelled && ref.current) {
          ref.current.innerHTML = svg;
        }
      })
      .catch((err: Error) => {
        if (!cancelled) {
          setError(err.message || "Mermaid 渲染失败");
        }
      });
    return () => {
      cancelled = true;
    };
  }, [code, id]);

  if (error) {
    return (
      <div className="my-2 p-3 rounded border border-red-300 bg-red-50 text-red-600 text-xs">
        <div className="font-semibold mb-1">Mermaid 图表渲染失败：</div>
        <pre className="whitespace-pre-wrap">{error}</pre>
      </div>
    );
  }

  return (
    <div
      ref={ref}
      className="my-2 flex justify-center overflow-x-auto bg-white dark:bg-zinc-900 rounded border border-gray-200 dark:border-zinc-700 p-3"
    />
  );
};

/**
 * 代码块组件：自动高亮 + 复制按钮。
 * 用于替换 XMarkdown 解析出来的 <pre> 标签。
 */
export const PreBlock: React.FC<ComponentProps> = ({
  domNode,
  streamStatus: _streamStatus,
  ...rest
}) => {
  const codeRef = useRef<HTMLElement>(null);
  const [copied, setCopied] = useState(false);

  // 从 domNode 解析语言和代码内容
  type ParserNode = {
    name?: string;
    attribs?: { class?: string };
    children?: ParserNode[];
    data?: string;
  };
  const codeNode = (domNode as ParserNode)?.children?.find(
    (c) => c.name === "code",
  );
  const language = getLanguage(codeNode?.attribs?.class);

  // 从原始 DOM 节点递归提取纯文本
  const extractText = (node: ParserNode | undefined): string => {
    if (!node) return "";
    if (node.data) return node.data;
    if (node.children) return node.children.map(extractText).join("");
    return "";
  };
  const rawCode = extractText(codeNode).replace(/\n$/, "");

  useEffect(() => {
    if (codeRef.current && language && language !== "mermaid") {
      try {
        codeRef.current.removeAttribute("data-highlighted");
        hljs.highlightElement(codeRef.current);
      } catch {
        // ignore
      }
    }
  }, [rawCode, language]);

  const handleCopy = () => {
    navigator.clipboard
      .writeText(rawCode)
      .then(() => {
        setCopied(true);
        antdMessage.success("已复制");
        setTimeout(() => setCopied(false), 1500);
      })
      .catch(() => antdMessage.error("复制失败"));
  };

  // Mermaid 特殊渲染
  if (language === "mermaid") {
    return <MermaidBlock code={rawCode} />;
  }

  return (
    <div className="relative my-2 group">
      <div className="flex items-center justify-between px-3 py-1.5 bg-zinc-800 text-gray-300 text-xs rounded-t-md">
        <span className="font-mono">{language || "text"}</span>
        <Button
          type="text"
          size="small"
          icon={copied ? <CheckOutlined /> : <CopyOutlined />}
          onClick={handleCopy}
          className="!text-gray-300 hover:!text-white !h-6"
        >
          {copied ? "已复制" : "复制"}
        </Button>
      </div>
      <pre
        {...rest}
        className="!m-0 !rounded-t-none !rounded-b-md overflow-x-auto"
      >
        <code
          ref={codeRef}
          className={language ? `language-${language} hljs` : "hljs"}
        >
          {rawCode}
        </code>
      </pre>
    </div>
  );
};

export default PreBlock;
