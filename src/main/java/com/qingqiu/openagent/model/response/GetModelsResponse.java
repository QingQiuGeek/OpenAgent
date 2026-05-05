package com.qingqiu.openagent.model.response;

import com.qingqiu.openagent.model.vo.ModelVO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetModelsResponse {

    private List<ModelVO> models;
}
