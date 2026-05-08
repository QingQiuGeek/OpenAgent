package com.qingqiu.openagent.model.request;

import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/12 13:03
 * @description: UpdateMcpServer request payload
 */

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
