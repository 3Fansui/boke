package com.boke.db.service.impl;

import com.boke.db.mapper.UserRoleMapper;
import com.boke.db.entity.UserRole;
import com.boke.db.service.UserRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {

}
