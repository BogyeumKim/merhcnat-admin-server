package com.app.demo.notice.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** notice 테이블 CRUD (MyBatis-Plus BaseMapper) */
@Mapper
public interface NoticeMapper extends BaseMapper<NoticeEntity> {
}
