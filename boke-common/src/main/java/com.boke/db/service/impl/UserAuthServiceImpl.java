package com.boke.db.service.impl;

import com.alibaba.fastjson.JSON;
import com.boke.constant.CommonConstant;
import com.boke.constant.RabbitMQConstant;
import com.boke.constant.RedisConstant;
import com.boke.db.mapper.UserAuthMapper;
import com.boke.db.mapper.UserInfoMapper;
import com.boke.db.mapper.UserRoleMapper;
import com.boke.db.entity.UserAuth;
import com.boke.db.entity.UserInfo;
import com.boke.db.entity.UserRole;
import com.boke.enums.LoginTypeEnum;
import com.boke.enums.RoleEnum;
import com.boke.enums.UserAreaTypeEnum;
import com.boke.exception.BizException;
import com.boke.db.service.bokeInfoService;
import com.boke.db.service.RedisService;
import com.boke.db.service.TokenService;
import com.boke.db.service.UserAuthService;
import com.boke.model.dto.*;
import com.boke.model.vo.ConditionVO;
import com.boke.model.vo.PasswordVO;
import com.boke.model.vo.QQLoginVO;
import com.boke.model.vo.UserVO;
import com.boke.strategy.context.SocialLoginStrategyContext;
import com.boke.util.PageUtil;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.boke.util.UserUtil;
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

import static com.boke.enums.UserAreaTypeEnum.getUserAreaType;
import static com.boke.util.CommonUtil.checkEmail;
import static com.boke.util.CommonUtil.getRandomCode;

 /**
 * 用户认证服务实现类
 * 主要功能：
 * 1. 用户注册与认证
 * 2. 邮箱验证码管理
 * 3. 密码管理
 * 4. 用户地区统计
 * 5. 社交登录集成
 */
@Service
public class UserAuthServiceImpl implements UserAuthService {

    /**
     * 用户认证数据访问接口
     */
    @Autowired
    private UserAuthMapper userAuthMapper;

    /**
     * 用户信息数据访问接口
     */
    @Autowired
    private UserInfoMapper userInfoMapper;

    /**
     * 用户角色关联数据访问接口
     */
    @Autowired
    private UserRoleMapper userRoleMapper;

    /**
     * Redis服务接口
     */
    @Autowired
    private RedisService redisService;

    /**
     * 博客信息服务接口
     */
    @Autowired
    private bokeInfoService bokeInfoService;

    /**
     * Token服务接口
     */
    @Autowired
    private TokenService tokenService;

    /**
     * RabbitMQ模板
     */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 社交登录策略上下文
     */
    @Autowired
    private SocialLoginStrategyContext socialLoginStrategyContext;

    /**
     * 发送邮箱验证码
     * 实现流程：
     * 1. 校验邮箱格式
     * 2. 生成随机验证码
     * 3. 构建邮件内容
     * 4. 通过消息队列异步发送邮件
     * 5. 将验证码保存到Redis
     * @param username 用户邮箱
     * @throws BizException 当邮箱格式不正确时抛出异常
     */
    @Override
    public void sendCode(String username) {
        // 邮箱格式校验
        if (!checkEmail(username)) {
            throw new BizException("请输入正确邮箱");
        }

        // 生成6位随机验证码
        String code = getRandomCode();

        // 构建邮件内容，使用模板
        Map<String, Object> map = new HashMap<>();
        map.put("content", "您的验证码为 " + code + " 有效期15分钟，请不要告诉他人哦！");
        EmailDTO emailDTO = EmailDTO.builder()
                .email(username)
                .subject(CommonConstant.CAPTCHA)
                .template("common.html")  // 使用通用邮件模板
                .commentMap(map)
                .build();

        // 通过RabbitMQ发送邮件，提高系统响应速度
        rabbitTemplate.convertAndSend(
                RabbitMQConstant.EMAIL_EXCHANGE,
                "*",
                new Message(JSON.toJSONBytes(emailDTO), new MessageProperties())
        );

        // 将验证码保存到Redis，设置15分钟过期时间
        redisService.set(RedisConstant.USER_CODE_KEY + username, code, RedisConstant.CODE_EXPIRE_TIME);
    }

    /**
     * 获取用户地区统计信息
     * 用户地区统计
     * 支持两种统计类型：
     * 1. 注册用户地区分布
     * 2. 访客地区分布
     * @param conditionVO 查询条件
     * @return 用户地区统计列表
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<UserAreaDTO> listUserAreas(ConditionVO conditionVO) {
        List<UserAreaDTO> userAreaDTOs = new ArrayList<>();

        // 根据统计类型获取不同的地区数据
        switch (Objects.requireNonNull(getUserAreaType(conditionVO.getType()))) {
            case USER:
                // 从Redis获取用户地区统计数据
                Object userArea = redisService.get(RedisConstant.USER_AREA);
                if (Objects.nonNull(userArea)) {
                    userAreaDTOs = JSON.parseObject(userArea.toString(), List.class);
                }
                return userAreaDTOs;

            case VISITOR:
                // 从Redis获取访客地区统计数据
                Map<String, Object> visitorArea = redisService.hGetAll(RedisConstant.VISITOR_AREA);
                if (Objects.nonNull(visitorArea)) {
                    // 将Map转换为DTO列表
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

    /**
     * 用户注册
     * 实现流程：
     * 1. 校验邮箱格式和验证码
     * 2. 检查用户是否已存在
     * 3. 创建用户基本信息
     * 4. 分配用户角色
     * 5. 创建认证信息
     * @param userVO 用户注册信息
     * @throws BizException 当邮箱格式不正确或已被注册时抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserVO userVO) {
        // 邮箱格式校验
        if (!checkEmail(userVO.getUsername())) {
            throw new BizException("邮箱格式不对!");
        }

        // 验证码校验和用户查重
        if (checkUser(userVO)) {
            throw new BizException("邮箱已被注册！");
        }

        // 创建用户基本信息
        UserInfo userInfo = UserInfo.builder()
                .email(userVO.getUsername())
                .nickname(CommonConstant.DEFAULT_NICKNAME + IdWorker.getId())  // 生成默认昵称
                .avatar(bokeInfoService.getWebsiteConfig().getUserAvatar())   // 设置默认头像
                .build();
        userInfoMapper.insert(userInfo);

        // 分配用户角色
        UserRole userRole = UserRole.builder()
                .userId(userInfo.getId())
                .roleId(RoleEnum.USER.getRoleId())  // 默认分配普通用户角色
                .build();
        userRoleMapper.insert(userRole);

        // 创建认证信息，密码加密存储
        UserAuth userAuth = UserAuth.builder()
                .userInfoId(userInfo.getId())
                .username(userVO.getUsername())
                .password(BCrypt.hashpw(userVO.getPassword(), BCrypt.gensalt()))  // 使用BCrypt加密
                .loginType(LoginTypeEnum.EMAIL.getType())
                .build();
        userAuthMapper.insert(userAuth);
    }

    /**
     * 修改密码
     *
     * @param userVO 用户信息
     * @throws BizException 当邮箱未注册时抛出异常
     */
    @Override
    public void updatePassword(UserVO userVO) {
        // 检查用户是否存在
        if (!checkUser(userVO)) {
            throw new BizException("邮箱尚未注册！");
        }
        // 更新密码
        userAuthMapper.update(new UserAuth(), new LambdaUpdateWrapper<UserAuth>()
                .set(UserAuth::getPassword, BCrypt.hashpw(userVO.getPassword(), BCrypt.gensalt()))
                .eq(UserAuth::getUsername, userVO.getUsername()));
    }

    /**
     * 修改管理员密码
     *
     * @param passwordVO 密码信息
     * @throws BizException 当旧密码不正确时抛出异常
     */
    @Override
    @SuppressWarnings("all")
    public void updateAdminPassword(PasswordVO passwordVO) {
        // 验证旧密码
        UserAuth user = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>()
                .eq(UserAuth::getId, UserUtil.getUserDetailsDTO().getId()));
        if (Objects.nonNull(user) && BCrypt.checkpw(passwordVO.getOldPassword(), user.getPassword())) {
            // 更新新密码
            UserAuth userAuth = UserAuth.builder()
                    .id(UserUtil.getUserDetailsDTO().getId())
                    .password(BCrypt.hashpw(passwordVO.getNewPassword(), BCrypt.gensalt()))
                    .build();
            userAuthMapper.updateById(userAuth);
        } else {
            throw new BizException("旧密码不正确");
        }
    }

    /**
     * 查询后台用户列表
     *
     * @param conditionVO 条件
     * @return 用户列表
     */
    @Override
    public PageResultDTO<UserAdminDTO> listUsers(ConditionVO conditionVO) {
        // 查询用户数量
        Integer count = userAuthMapper.countUser(conditionVO);
        if (count == 0) {
            return new PageResultDTO<>();
        }
        // 分页查询用户列表
        List<UserAdminDTO> UserAdminDTOs = userAuthMapper.listUsers(
                PageUtil.getLimitCurrent(),
                PageUtil.getSize(),
                conditionVO);
        return new PageResultDTO<>(UserAdminDTOs, count);
    }

    /**
     * 用户注销
     *
     * @return 注销状态
     */
    @SneakyThrows
    @Override
    public UserLogoutStatusDTO logout() {
        // 删除用户登录信息
        tokenService.delLoginUser(UserUtil.getUserDetailsDTO().getId());
        return new UserLogoutStatusDTO("注销成功");
    }

    /**
     * QQ登录
     *
     * @param qqLoginVO QQ登录信息
     * @return 用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserInfoDTO qqLogin(QQLoginVO qqLoginVO) {
        return socialLoginStrategyContext.executeLoginStrategy(
                JSON.toJSONString(qqLoginVO),
                LoginTypeEnum.QQ);
    }

    /**
     * 校验用户是否存在
     *
     * @param user 用户信息
     * @return 用户是否存在
     * @throws BizException 当验证码错误时抛出异常
     */
    private Boolean checkUser(UserVO user) {
        // 验证验证码
        if (!user.getCode().equals(redisService.get(RedisConstant.USER_CODE_KEY + user.getUsername()))) {
            throw new BizException("验证码错误！");
        }
        // 查询用户是否存在
        UserAuth userAuth = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>()
                .select(UserAuth::getUsername)
                .eq(UserAuth::getUsername, user.getUsername()));
        return Objects.nonNull(userAuth);
    }

}
