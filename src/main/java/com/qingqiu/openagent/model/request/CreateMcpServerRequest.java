package com.qingqiu.openagent.model.request;

import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/1 20:36
 * @description: CreateMcpServer request payload
 */

@Data
public class CreateMcpServerRequest {

    private String name;

    /** stdio / sse / http */
    private String transport;

    private String command;

    private String url;

    /** JSON string */
    private String headers;

    /** JSON string */
    private String env;

    private Integer enabled;
}
