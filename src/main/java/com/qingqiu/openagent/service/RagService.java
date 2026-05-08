package com.qingqiu.openagent.service;

import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/2 18:06
 * @description: Rag service
 */

public interface RagService {
    float[] embed(String text);

    List<String> similaritySearch(String kbId, String title);
}
