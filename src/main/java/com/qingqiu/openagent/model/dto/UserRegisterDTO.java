package com.qingqiu.openagent.model.dto;

import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/6 16:26
 * @description: UserRegister DTO
 */

@Data
public class UserRegisterDTO {

    private String mail;

    private String password;

    private String rePassword;

    private String code;
}
