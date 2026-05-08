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
 * @date: 2026/5/10 09:17
 * @description: Model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("model")
public class Model {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String modelName;

    private String providerType;

    private String baseUrl;

    private String apiKey;

    private Integer maxTokens;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
