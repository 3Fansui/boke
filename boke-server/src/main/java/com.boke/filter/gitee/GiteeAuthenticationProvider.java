package com.boke.filter.gitee;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boke.entity.UserAuth;
import com.boke.entity.UserInfo;
import com.boke.entity.UserRole;
import com.boke.enums.RoleEnum;
import com.boke.mapper.UserAuthMapper;
import com.boke.mapper.UserInfoMapper;
import com.boke.mapper.UserRoleMapper;
import com.boke.model.dto.SocialTokenDTO;
import com.boke.model.dto.SocialUserInfoDTO;
import com.boke.model.dto.UserDetailsDTO;
import com.boke.model.dto.UserInfoDTO;
import com.boke.service.TokenService;
import com.boke.service.impl.UserDetailServiceImpl;
import com.boke.util.BeanCopyUtil;
import com.boke.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class GiteeAuthenticationProvider implements AuthenticationProvider {


    private final UserAuthMapper userAuthMapper;
    private final UserInfoMapper userInfoMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserDetailServiceImpl userDetailService;
    private final HttpServletRequest request;
    private final GiteeApiClient giteeApiClient;

    @Autowired
    public GiteeAuthenticationProvider(UserAuthMapper userAuthMapper,
                                       UserInfoMapper userInfoMapper,
                                       UserRoleMapper userRoleMapper,
                                       UserDetailServiceImpl userDetailService,
                                       HttpServletRequest request,
                                       GiteeApiClient giteeApiClient) {
        this.userAuthMapper = userAuthMapper;
        this.userInfoMapper = userInfoMapper;
        this.userRoleMapper = userRoleMapper;
        this.userDetailService = userDetailService;
        this.request = request;
        this.giteeApiClient = giteeApiClient;
    }


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String code = authentication.getCredentials().toString();
        try {
            String token = giteeApiClient.getTokenByCode(code);
            if (token == null) {
                // 乱传code过来。用户根本没授权！
                throw new BadCredentialsException("Gitee账号授权失败！");
            }
            Map<String, Object> thirdUser = giteeApiClient.getThirdUserInfo(token);
            if (thirdUser == null) {
                // 未知异常。获取不到用户openId，也就无法继续登录了
                throw new BadCredentialsException("Gitee账号授权失败！");
            }
            String openId = thirdUser.get("openId").toString();
            String username = thirdUser.get("nickname").toString();
            String ipAddress = IpUtil.getIpAddress(request);
            String ipSource = IpUtil.getIpSource(ipAddress);
            // 通过第三方的账号唯一id，去匹配数据库中已有的账号信息
            UserAuth user = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>().eq(UserAuth::getUsername, openId));
            boolean notBindAccount = user == null; // gitee账号没有绑定我们系统的用户
            UserDetailsDTO userDetail;
            if (notBindAccount) {
                // 没找到账号信息，那就是第一次使用gitee登录，需要创建一个新用户
                userDetail=saveUserDetail(username, openId, ipAddress, ipSource);
            }else{
                userDetail=userDetailService.convertUserDetail(user,request);
            }

            GiteeAuthentication successAuth = new GiteeAuthentication();
            successAuth.setCurrentUser(userDetail);
            successAuth.setAuthenticated(true); // 认证通过，一定要设成true

            /*HashMap<String, Object> loginDetail = new HashMap<>();
            // 第一次使用三方账号登录，需要告知前端，让前端跳转到初始化账号页面（可能需要）
            loginDetail.put("needInitUserInfo", notBindAccount);
            loginDetail.put("nickname", thirdUser.get("nickname").toString()); // sayHello*/
            successAuth.setDetails(userDetail);
            return successAuth;

        } catch (Exception e) {
            // 未知异常
            throw new BadCredentialsException("Gitee账号认证失败！");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return GiteeAuthentication.class.isAssignableFrom(authentication);
    }

    private UserDetailsDTO saveUserDetail(String username,String openID, String ipAddress, String ipSource) {

        UserInfo userInfo = UserInfo.builder()
                .nickname(username)
                .avatar("")
                .build();
        userInfoMapper.insert(userInfo);
        UserAuth userAuth = UserAuth.builder()
                .userInfoId(userInfo.getId())
                //TODO 这里暂且把openId作为用户username，因为数据库表user_auth中没有openId这个字段
                .username(openID)
                //默认密码123456
                .password("$2a$10$/Z90STxVyGOIfNhTfvzbEuJ9t1yHjrkN6pBMRAqd5g5SdNIrdt5Da")
                .loginType(3)
                .lastLoginTime(LocalDateTime.now())
                .ipAddress(ipAddress)
                .ipSource(ipSource)
                .build();
        userAuthMapper.insert(userAuth);
        UserRole userRole = UserRole.builder()
                .userId(userInfo.getId())
                .roleId(RoleEnum.USER.getRoleId())
                .build();
        userRoleMapper.insert(userRole);
        return userDetailService.convertUserDetail(userAuth, request);
    }

}
