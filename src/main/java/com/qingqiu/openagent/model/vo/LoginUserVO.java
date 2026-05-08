package com.qingqiu.openagent.model.vo;

import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 13:07
 * @description: LoginUser view object
 */

@Data
public class LoginUserVO {

    private Long userId;

    private String userName;

    private String mail;

    private String role;
}
