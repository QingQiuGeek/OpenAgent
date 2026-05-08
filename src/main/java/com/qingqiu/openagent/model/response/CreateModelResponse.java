package com.qingqiu.openagent.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: qingqiugeek
 * @date: 2026/5/4 21:57
 * @description: CreateModel response payload
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateModelResponse {

    private Long modelId;
}
