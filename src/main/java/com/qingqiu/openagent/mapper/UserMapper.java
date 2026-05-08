package com.qingqiu.openagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingqiu.openagent.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author: qingqiugeek
 * @date: 2026/5/6 12:26
 * @description: User MyBatis mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
