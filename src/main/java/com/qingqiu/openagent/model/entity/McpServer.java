package com.qingqiu.openagent.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 16:57
 * @description: McpServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("mcp_server")
public class McpServer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    /** stdio / sse / http */
    private String transport;

    /** stdio 时的启动命令（如 "npx -y @modelcontextprotocol/server-github"）。 */
    private String command;

    /** sse / http 时的访问地址。 */
    private String url;

    /** JSON string：鉴权头等。 */
    private String headers;

    /** JSON string：环境变量。 */
    private String env;

    /** 0 禁用 / 1 启用。 */
    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
