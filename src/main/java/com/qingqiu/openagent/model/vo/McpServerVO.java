package com.qingqiu.openagent.model.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServerVO {

    private Long id;

    private Long userId;

    private String name;

    private String transport;

    private String command;

    private String url;

    /** JSON string */
    private String headers;

    /** JSON string */
    private String env;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
