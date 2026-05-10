package com.qingqiu.openagent.model.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 14:03
 * @description: ShareLink view object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareLinkVO {

    private String id;
    private String slug;
    private String sessionId;
    private LocalDateTime expireAt;
    private Integer viewCount;
    private LocalDateTime createdAt;
}
