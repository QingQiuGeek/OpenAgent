package com.qingqiu.openagent.model.response;

import com.qingqiu.openagent.model.vo.ModelVO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: qingqiugeek
 * @date: 2026/5/6 11:48
 * @description: GetModels response payload
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetModelsResponse {

    private List<ModelVO> models;
}
