package com.qingqiu.openagent.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentVO {
    private String id;
    private Long userId;
    private String kbId;
    private String filename;
    private String filetype;
    private Long size;
}

