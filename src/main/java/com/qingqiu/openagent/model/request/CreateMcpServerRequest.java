package com.qingqiu.openagent.model.request;

import lombok.Data;

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
