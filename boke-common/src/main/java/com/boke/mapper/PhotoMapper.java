package com.boke.mapper;

import com.boke.entity.Photo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
public interface PhotoMapper extends BaseMapper<Photo> {

}
