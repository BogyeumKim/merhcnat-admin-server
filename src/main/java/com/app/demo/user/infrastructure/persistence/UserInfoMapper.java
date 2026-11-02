package com.app.demo.user.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** user_info 테이블 CRUD (MyBatis-Plus BaseMapper) */
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfoEntity> {
}
