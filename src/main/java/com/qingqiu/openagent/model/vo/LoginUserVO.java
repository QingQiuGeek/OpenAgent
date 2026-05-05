package com.qingqiu.openagent.model.vo;

import lombok.Data;

@Data
public class LoginUserVO {

    private Long userId;

    private String userName;

    private String mail;

    private String role;
}
