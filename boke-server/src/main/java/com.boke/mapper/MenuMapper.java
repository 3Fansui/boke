package com.boke.mapper;

import com.boke.entity.Menu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuMapper extends BaseMapper<Menu> {

    List<Menu> listMenusByUserInfoId(Integer userInfoId);

}
