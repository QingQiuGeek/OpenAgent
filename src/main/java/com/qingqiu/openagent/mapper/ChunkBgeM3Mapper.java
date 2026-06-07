package com.qingqiu.openagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingqiu.openagent.model.entity.ChunkBgeM3;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/2 22:19
 * @description: ChunkBgeM3 MyBatis mapper
 */
@Mapper
public interface ChunkBgeM3Mapper extends BaseMapper<ChunkBgeM3> {

    List<ChunkBgeM3> similaritySearch(
            @Param("kbId") String kbId,
            @Param("vectorLiteral") String vectorLiteral,
            @Param("limit") int limit
    );

    /**
     * 按 docId 物理删除该文档的所有 chunks（用于文档删除/重建场景）
     */
    int deleteByDocId(@Param("docId") String docId);

    /**
     * 批量插入 chunks
     */
    int batchInsert(@Param("chunks") List<ChunkBgeM3> chunks);
}
