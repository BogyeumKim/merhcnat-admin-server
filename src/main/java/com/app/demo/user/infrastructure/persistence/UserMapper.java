package com.app.demo.user.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** user 테이블 CRUD (MyBatis-Plus BaseMapper) */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
