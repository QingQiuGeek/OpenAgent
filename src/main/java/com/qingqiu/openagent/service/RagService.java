package com.qingqiu.openagent.service;

import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/2 18:06
 * @description: Rag service
 */

public interface RagService {
    float[] embed(String text);

    /**
     * 批量向量化（内部自动分批，每批 ≤10 条）
     */
    List<float[]> batchEmbed(List<String> texts);

    List<String> similaritySearch(String kbId, String title);
}
