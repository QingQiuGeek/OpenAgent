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
}
