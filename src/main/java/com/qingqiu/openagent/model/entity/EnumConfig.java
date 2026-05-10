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
 * @date: 2026/5/2 18:09
 * @description: Enum configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("enum_config")
public class EnumConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 枚举类别：model_provider_type / tool_type / document_filetype / mcp_transport ... */
    private String type;

    /** 枚举值：openai / pdf / stdio ... */
    private String value;

    /** 0 正常 / 1 禁用。 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
