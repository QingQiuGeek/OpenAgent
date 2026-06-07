package com.qingqiu.openagent.service.impl;

import com.qingqiu.openagent.mapper.ChunkBgeM3Mapper;
import com.qingqiu.openagent.model.entity.ChunkBgeM3;
import com.qingqiu.openagent.service.RagService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/10 18:53
 * @description: Rag service implementation
 */

@Service
public class RagServiceImpl implements RagService {

    // 由 langchain4j-community-dashscope-spring-boot-starter 根据 yaml
    // langchain4j.community.dashscope.embedding-model 配置自动装配
    private final EmbeddingModel embeddingModel;
    private final ChunkBgeM3Mapper chunkBgeM3Mapper;

    public RagServiceImpl(EmbeddingModel embeddingModel, ChunkBgeM3Mapper chunkBgeM3Mapper) {
        this.embeddingModel = embeddingModel;
        this.chunkBgeM3Mapper = chunkBgeM3Mapper;
    }

    /**
     * 使用 yaml 中配置的向量模型（DashScope text-embedding-v3，1024 维）进行文本向量化
     * @param text 待向量化文本
     * @return 向量数组
     */
    private float[] doEmbed(String text) {
        Assert.hasText(text, "Embedding text cannot be empty");
        Embedding embedding = embeddingModel.embed(text).content();
        Assert.notNull(embedding, "Embedding response cannot be null");
        return embedding.vector();
    }

    @Override
    public float[] embed(String text) {
        return doEmbed(text);
    }

    /** DashScope text-embedding-v3 单次批量上限 */
    private static final int BATCH_SIZE = 10;

    @Override
    public List<float[]> batchEmbed(List<String> texts) {
        if (texts == null || texts.isEmpty()) return List.of();

        List<float[]> allEmbeddings = new ArrayList<>();
        // 按 BATCH_SIZE 分批调用
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, texts.size());
            List<TextSegment> batch = texts.subList(i, end).stream()
                    .map(TextSegment::from)
                    .toList();
            List<Embedding> batchResult = embeddingModel.embedAll(batch).content();
            batchResult.forEach(e -> allEmbeddings.add(e.vector()));
        }
        return allEmbeddings;
    }

    /**
     * 相似性检索
     * @param kbId
     * @param title
     * @return
     */
    @Override
    public List<String> similaritySearch(String kbId, String title) {
        String queryEmbedding = toPgVector(doEmbed(title));
        List<ChunkBgeM3> chunks = chunkBgeM3Mapper.similaritySearch(kbId, queryEmbedding, 3);
        return chunks.stream().map(ChunkBgeM3::getContent).toList();
    }

    /**
     * pgvector 使用字符串表示向量，例如 [1.0, 2.0, 3.0]，需要拼接成特定格式
     * @param v
     * @return
     */
    private String toPgVector(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            sb.append(v[i]);
            if (i < v.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
