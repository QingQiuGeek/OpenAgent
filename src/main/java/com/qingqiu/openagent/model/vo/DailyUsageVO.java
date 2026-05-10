package com.qingqiu.openagent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 10:02
 * @description: 按日聚合的用量视图（折线图）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyUsageVO {

    /** yyyy-MM-dd */
    private String date;
    private long calls;
    private long totalTokens;
}
