package com.qingqiu.openagent.model.response;

import com.qingqiu.openagent.model.vo.McpServerVO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetMcpServersResponse {
    private List<McpServerVO> mcpServers;
}
