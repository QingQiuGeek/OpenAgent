package com.qingqiu.openagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingqiu.openagent.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper。基于 MyBatis-Plus BaseMapper，使用逻辑删除字段 {@code is_deleted}。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
