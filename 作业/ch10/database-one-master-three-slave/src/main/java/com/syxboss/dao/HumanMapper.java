package com.syxboss.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syxboss.entity.Human;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface HumanMapper extends BaseMapper<Human> {
}
