package com.boke.service.impl;


import com.boke.strategy.UserDetailsDTO;
import com.boke.entity.UserAuth;
import com.boke.entity.UserInfo;
import com.boke.exception.BizException;
import com.boke.mapper.RoleMapper;
import com.boke.mapper.UserAuthMapper;
import com.boke.mapper.UserInfoMapper;
import com.boke.util.IpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class UserDetailServiceImpl implements UserDetailsService {

    @Autowired
    private UserAuthMapper userAuthMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Resource
    private HttpServletRequest request;

    @SneakyThrows
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (StringUtils.isBlank(username)) {
            throw new BizException("用户名不能为空!");
        }
        UserAuth userAuth = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>()
                .select(UserAuth::getId, UserAuth::getUserInfoId, UserAuth::getUsername, UserAuth::getPassword, UserAuth::getLoginType)
                .eq(UserAuth::getUsername, username));
        if (Objects.isNull(userAuth)) {
            throw new BizException("用户不存在!");
        }
        return convertUserDetail(userAuth, request);
    }

    public UserDetailsDTO convertUserDetail(UserAuth user, HttpServletRequest request) {
        UserInfo userInfo = userInfoMapper.selectById(user.getUserInfoId());
        List<String> roles = roleMapper.listRolesByUserInfoId(userInfo.getId());
        String ipAddress = IpUtil.getIpAddress(request);
        String ipSource = IpUtil.getIpSource(ipAddress);
        UserAgent userAgent = IpUtil.getUserAgent(request);
        return UserDetailsDTO.builder()
                .id(user.getId())
                .loginType(user.getLoginType())
                .userInfoId(userInfo.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(userInfo.getEmail())
                .roles(roles)
                .nickname(userInfo.getNickname())
                .avatar(userInfo.getAvatar())
                .intro(userInfo.getIntro())
                .website(userInfo.getWebsite())
                .isSubscribe(userInfo.getIsSubscribe())
                .ipAddress(ipAddress)
                .ipSource(ipSource)
                .isDisable(userInfo.getIsDisable())
                .browser(userAgent.getBrowser().getName())
                .os(userAgent.getOperatingSystem().getName())
                .lastLoginTime(LocalDateTime.now())
                .build();
    }

}
