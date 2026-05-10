package com.qingqiu.openagent.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/12 19:26
 * @description: Document view object
 */

@Data
@Builder
public class DocumentVO {
    private String id;
    private Long userId;
    private String kbId;
    private String filename;
    private String filetype;
    private Long size;
    /** 处理状态：uploading / vectorizing / done / failed / skipped */
    private String status;
    /** 失败原因 */
    private String errorMsg;
}

