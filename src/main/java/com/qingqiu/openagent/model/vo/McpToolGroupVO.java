package com.qingqiu.openagent.model.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 16:30
 * @description: MCP server 暴露的一组工具，用于前端 Agent 编辑页分组渲染
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolGroupVO {

    private Long mcpServerId;
    private String mcpServerName;
    private String transport;
    /** 仅工具名（前端展示用）。 */
    private List<String> toolNames;
    /** 加载是否成功；失败时 toolNames 为空，errorMsg 描述原因 */
    private boolean ok;
    private String errorMsg;
}
