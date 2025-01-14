package com.boke.service.impl;

import com.alibaba.fastjson.JSON;
import com.boke.constant.CommonConstant;
import com.boke.model.dto.*;
import com.boke.entity.UserAuth;
import com.boke.entity.UserInfo;
import com.boke.entity.UserRole;
import com.boke.enums.LoginTypeEnum;
import com.boke.enums.RoleEnum;
import com.boke.exception.BizException;
import com.boke.mapper.UserAuthMapper;
import com.boke.mapper.UserInfoMapper;
import com.boke.mapper.UserRoleMapper;
import com.boke.service.bokeInfoService;
import com.boke.service.RedisService;
import com.boke.service.TokenService;
import com.boke.service.UserAuthService;
import com.boke.strategy.UserUtil;
import com.boke.strategy.context.SocialLoginStrategyContext;
import com.boke.util.PageUtil;

import com.boke.model.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.boke.constant.RabbitMQConstant.EMAIL_EXCHANGE;
import static com.boke.constant.RedisConstant.*;
import static com.boke.enums.UserAreaTypeEnum.getUserAreaType;
import static com.boke.util.CommonUtil.checkEmail;
import static com.boke.util.CommonUtil.getRandomCode;


@Service
public class UserAuthServiceImpl implements UserAuthService {

    @Autowired
    private UserAuthMapper userAuthMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private bokeInfoService bokeInfoService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SocialLoginStrategyContext socialLoginStrategyContext;

    @Override
    public void sendCode(String username) {
        if (!checkEmail(username)) {
            throw new BizException("请输入正确邮箱");
        }
        String code = getRandomCode();
        Map<String, Object> map = new HashMap<>();
        map.put("content", "您的验证码为 " + code + " 有效期15分钟，请不要告诉他人哦！");
        EmailDTO emailDTO = EmailDTO.builder()
                .email(username)
                .subject(CommonConstant.CAPTCHA)
                .template("common.html")
                .commentMap(map)
                .build();
        rabbitTemplate.convertAndSend(EMAIL_EXCHANGE, "*", new Message(JSON.toJSONBytes(emailDTO), new MessageProperties()));
        redisService.set(USER_CODE_KEY + username, code, CODE_EXPIRE_TIME);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<UserAreaDTO> listUserAreas(ConditionVO conditionVO) {
        List<UserAreaDTO> userAreaDTOs = new ArrayList<>();
        switch (Objects.requireNonNull(getUserAreaType(conditionVO.getType()))) {
            case USER:
                Object userArea = redisService.get(USER_AREA);
                if (Objects.nonNull(userArea)) {
                    userAreaDTOs = JSON.parseObject(userArea.toString(), List.class);
                }
                return userAreaDTOs;
            case VISITOR:
                Map<String, Object> visitorArea = redisService.hGetAll(VISITOR_AREA);
                if (Objects.nonNull(visitorArea)) {
                    userAreaDTOs = visitorArea.entrySet().stream()
                            .map(item -> UserAreaDTO.builder()
                                    .name(item.getKey())
                                    .value(Long.valueOf(item.getValue().toString()))
                                    .build())
                            .collect(Collectors.toList());
                }
                return userAreaDTOs;
            default:
                break;
        }
        return userAreaDTOs;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserVO userVO) {
        if (!checkEmail(userVO.getUsername())) {
            throw new BizException("邮箱格式不对!");
        }
        if (checkUser(userVO)) {
            throw new BizException("邮箱已被注册！");
        }
        UserInfo userInfo = UserInfo.builder()
                .email(userVO.getUsername())
                .nickname(CommonConstant.DEFAULT_NICKNAME + IdWorker.getId())
                .avatar(bokeInfoService.getWebsiteConfig().getUserAvatar())
                .build();
        userInfoMapper.insert(userInfo);
        UserRole userRole = UserRole.builder()
                .userId(userInfo.getId())
                .roleId(RoleEnum.USER.getRoleId())
                .build();
        userRoleMapper.insert(userRole);
        UserAuth userAuth = UserAuth.builder()
                .userInfoId(userInfo.getId())
                .username(userVO.getUsername())
                .password(BCrypt.hashpw(userVO.getPassword(), BCrypt.gensalt()))
                .loginType(LoginTypeEnum.EMAIL.getType())
                .build();
        userAuthMapper.insert(userAuth);
    }

    @Override
    public void updatePassword(UserVO userVO) {
        if (!checkUser(userVO)) {
            throw new BizException("邮箱尚未注册！");
        }
        userAuthMapper.update(new UserAuth(), new LambdaUpdateWrapper<UserAuth>()
                .set(UserAuth::getPassword, BCrypt.hashpw(userVO.getPassword(), BCrypt.gensalt()))
                .eq(UserAuth::getUsername, userVO.getUsername()));
    }

    @Override
    @SuppressWarnings("all")
    public void updateAdminPassword(PasswordVO passwordVO) {
        UserAuth user = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>()
                .eq(UserAuth::getId, UserUtil.getUserDetailsDTO().getId()));
        if (Objects.nonNull(user) && BCrypt.checkpw(passwordVO.getOldPassword(), user.getPassword())) {
            UserAuth userAuth = UserAuth.builder()
                    .id(UserUtil.getUserDetailsDTO().getId())
                    .password(BCrypt.hashpw(passwordVO.getNewPassword(), BCrypt.gensalt()))
                    .build();
            userAuthMapper.updateById(userAuth);
        } else {
            throw new BizException("旧密码不正确");
        }
    }

    @Override
    public PageResultDTO<UserAdminDTO> listUsers(ConditionVO conditionVO) {
        Integer count = userAuthMapper.countUser(conditionVO);
        if (count == 0) {
            return new PageResultDTO<>();
        }
        List<UserAdminDTO> UserAdminDTOs = userAuthMapper.listUsers(PageUtil.getLimitCurrent(), PageUtil.getSize(), conditionVO);
        return new PageResultDTO<>(UserAdminDTOs, count);
    }

    @SneakyThrows
    @Override
    public UserLogoutStatusDTO logout() {
        tokenService.delLoginUser(UserUtil.getUserDetailsDTO().getId());
        return new UserLogoutStatusDTO("注销成功");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserInfoDTO qqLogin(QQLoginVO qqLoginVO) {
        return socialLoginStrategyContext.executeLoginStrategy(JSON.toJSONString(qqLoginVO), LoginTypeEnum.QQ);
    }

    private Boolean checkUser(UserVO user) {
        if (!user.getCode().equals(redisService.get(USER_CODE_KEY + user.getUsername()))) {
            throw new BizException("验证码错误！");
        }
        UserAuth userAuth = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>()
                .select(UserAuth::getUsername)
                .eq(UserAuth::getUsername, user.getUsername()));
        return Objects.nonNull(userAuth);
    }

}
