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

    /** 枚举类别：model_provider_type / agent_status / ... */
    private String typeCode;

    /** 枚举项 code：openai / anthropic / ... */
    private String itemCode;

    /** 展示名。 */
    private String itemLabel;

    /** JSON string：扩展（图标、默认 base_url 等）。 */
    private String extra;

    private Integer sort;

    /** 0 正常 / 1 禁用。 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
