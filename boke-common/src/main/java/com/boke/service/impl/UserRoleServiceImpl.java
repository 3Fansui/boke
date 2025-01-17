package com.boke.service.impl;

import com.boke.entity.UserRole;
import com.boke.mapper.UserRoleMapper;
import com.boke.service.UserRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {

}
