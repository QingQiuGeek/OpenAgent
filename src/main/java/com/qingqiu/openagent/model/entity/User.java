package com.qingqiu.openagent.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: qingqiugeek
 * @date: 2026/5/3 11:41
 * @description: User
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("\"user\"")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String role;

    private String mail;

    private String phone;

    private String password;

    private String userName;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
