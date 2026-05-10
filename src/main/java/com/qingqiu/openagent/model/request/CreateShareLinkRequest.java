package com.qingqiu.openagent.model.request;

import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 14:02
 * @description: CreateShareLink request payload
 */
@Data
public class CreateShareLinkRequest {

    private String sessionId;

    /** 过期天数；null 或 <=0 表示永不过期 */
    private Integer expireDays;
}
