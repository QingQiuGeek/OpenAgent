import React, { useState } from "react";
import { Sender } from "@ant-design/x";

interface AgentChatInputProps {
  onSend: (message: string) => void;
  isAgentRunning?: boolean;
  onStop?: () => void;
}

const AgentChatInput: React.FC<AgentChatInputProps> = ({
  onSend,
  isAgentRunning = false,
  onStop,
}) => {
  const [message, setMessage] = useState("");

  return (
    <Sender
      loading={isAgentRunning}
      onSubmit={() => {
        if (isAgentRunning) return;
        onSend(message.trim());
        setMessage("");
      }}
      onCancel={() => {
        onStop?.();
      }}
      placeholder={isAgentRunning ? "智能体回复中..." : "输入消息..."}
      value={message}
      onChange={setMessage}
    />
  );
};

export default AgentChatInput;
