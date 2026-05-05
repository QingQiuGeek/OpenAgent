package com.qingqiu.openagent.model.request;

import lombok.Data;

@Data
public class UpdateMcpServerRequest {

    private String name;

    private String transport;

    private String command;

    private String url;

    private String headers;

    private String env;

    private Integer enabled;
}
