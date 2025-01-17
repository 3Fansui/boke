package com.boke.mapper;

import com.boke.entity.UserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

}
