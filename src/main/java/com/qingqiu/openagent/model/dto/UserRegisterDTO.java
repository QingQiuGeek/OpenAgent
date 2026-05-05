package com.qingqiu.openagent.model.dto;

import lombok.Data;

@Data
public class UserRegisterDTO {

    private String mail;

    private String password;

    private String rePassword;

    private String code;
}
