package com.boke.db.service.impl;

import com.alibaba.fastjson.JSON;
import com.boke.constant.CommonConstant;
import com.boke.constant.RabbitMQConstant;
import com.boke.db.mapper.ArticleMapper;
import com.boke.db.mapper.CommentMapper;
import com.boke.db.mapper.TalkMapper;
import com.boke.db.mapper.UserInfoMapper;
import com.boke.db.entity.Article;
import com.boke.db.entity.Comment;
import com.boke.db.entity.Talk;
import com.boke.db.entity.UserInfo;
import com.boke.enums.CommentTypeEnum;
import com.boke.exception.BizException;
import com.boke.db.service.bokeInfoService;
import com.boke.db.service.CommentService;
import com.boke.model.dto.*;
import com.boke.util.HTMLUtil;
import com.boke.util.PageUtil;

import com.boke.model.vo.CommentVO;
import com.boke.model.vo.ConditionVO;
import com.boke.model.vo.ReviewVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boke.util.UserUtil;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.boke.enums.CommentTypeEnum.getCommentEnum;

/**
 * 评论服务实现类
 *
 * 主要功能：
 * 1. 评论的增删改查操作
 * 2. 评论的审核与管理
 * 3. 评论回复的处理
 * 4. 评论邮件通知
 * 5. 评论数据统计
 *
 * 技术特点：
 * 1. 使用 MyBatis-Plus 进行数据访问
 * 2. 集成 RabbitMQ 处理异步邮件通知
 * 3. 使用 Thymeleaf 模板处理邮件内容
 * 4. 实现评论多级回复
 * 5. 支持评论内容安全过滤
 *
 * @author boke
 * @version 1.0
 * @since 2024-01-20
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    /**
     * 网站URL，用于在邮件通知中生成评论链接
     */
    @Value("${website.url}")
    private String websiteUrl;

    /**
     * 评论数据访问接口
     */
    @Autowired
    private CommentMapper commentMapper;

    /**
     * 文章数据访问接口
     */
    @Autowired
    private ArticleMapper articleMapper;

    /**
     * 说说数据访问接口
     */
    @Autowired
    private TalkMapper talkMapper;

    /**
     * 用户信息数据访问接口
     */
    @Autowired
    private UserInfoMapper userInfoMapper;

    /**
     * 博客信息服务接口
     */
    @Autowired
    private bokeInfoService bokeInfoService;

    /**
     * RabbitMQ模板，用于发送邮件消息
     */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 存储所有有效的评论类型
     * 在系统初始化时加载，用于评论类型的合法性校验
     */
    private static final List<Integer> types = new ArrayList<>();

    /**
     * 系统初始化时加载所有评论类型
     * 将枚举中定义的所有评论类型添加到types列表中
     */
    @PostConstruct
    public void init() {
        CommentTypeEnum[] values = CommentTypeEnum.values();
        for (CommentTypeEnum value : values) {
            types.add(value.getType());
        }
    }

    /**
     * 保存评论信息
     *
     * 处理流程：
     * 1. 校验评论数据的合法性
     * 2. 获取网站评论配置信息
     * 3. 过滤评论内容中的HTML标签
     * 4. 构建评论实体并保存
     * 5. 根据配置发送邮件通知
     *
     * @param commentVO 评论数据传输对象，包含评论的所有必要信息
     * @throws BizException 当评论数据验证失败时抛出
     */
    @Override
    public void saveComment(CommentVO commentVO) {
        // 校验评论数据
        checkCommentVO(commentVO);

        // 获取网站配置信息
        WebsiteConfigDTO websiteConfig = bokeInfoService.getWebsiteConfig();
        Integer isCommentReview = websiteConfig.getIsCommentReview();

        // 过滤HTML标签
        commentVO.setCommentContent(HTMLUtil.filter(commentVO.getCommentContent()));

        // 构建评论实体
        Comment comment = Comment.builder()
                .userId(UserUtil.getUserDetailsDTO().getUserInfoId())  // 设置评论用户ID
                .replyUserId(commentVO.getReplyUserId())              // 设置回复用户ID
                .topicId(commentVO.getTopicId())                      // 设置主题ID
                .commentContent(commentVO.getCommentContent())         // 设置评论内容
                .parentId(commentVO.getParentId())                    // 设置父评论ID
                .type(commentVO.getType())                            // 设置评论类型
                // 根据网站配置决定评论是否需要审核
                .isReview(isCommentReview == CommonConstant.TRUE ?
                        CommonConstant.FALSE : CommonConstant.TRUE)
                .build();

        // 保存评论
        commentMapper.insert(comment);

        // 获取评论者昵称
        String fromNickname = UserUtil.getUserDetailsDTO().getNickname();

        // 如果开启了邮件通知，异步发送通知邮件
        if (websiteConfig.getIsEmailNotice().equals(CommonConstant.TRUE)) {
            CompletableFuture.runAsync(() -> notice(comment, fromNickname));
        }
    }

    /**
     * 获取评论列表（分页）
     *
     * 处理流程：
     * 1. 查询一级评论总数
     * 2. 分页获取一级评论
     * 3. 查询并组装评论的回复数据
     * 4. 构建评论树形结构
     *
     * @param commentVO 评论查询条件，包含分页信息和主题ID
     * @return PageResultDTO<CommentDTO> 分页评论数据
     */
    @Override
    public PageResultDTO<CommentDTO> listComments(CommentVO commentVO) {
        // 查询评论总数（只统计一级评论）
        Integer commentCount = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Objects.nonNull(commentVO.getTopicId()), Comment::getTopicId, commentVO.getTopicId())
                .eq(Comment::getType, commentVO.getType())
                .isNull(Comment::getParentId)  // 一级评论
                .eq(Comment::getIsReview, CommonConstant.TRUE));  // 已审核的评论

        // 如果没有评论，返回空结果
        if (commentCount == 0) {
            return new PageResultDTO<>();
        }

        // 分页查询评论数据
        List<CommentDTO> commentDTOs = commentMapper.listComments(
                PageUtil.getLimitCurrent(),
                PageUtil.getSize(),
                commentVO);

        // 如果查询结果为空，返回空结果
        if (CollectionUtils.isEmpty(commentDTOs)) {
            return new PageResultDTO<>();
        }

        // 提取评论ID列表
        List<Integer> commentIds = commentDTOs.stream()
                .map(CommentDTO::getId)
                .collect(Collectors.toList());

        // 查询评论的回复数据
        List<ReplyDTO> replyDTOS = commentMapper.listReplies(commentIds);

        // 封装评论回复数据
        Map<Integer, List<ReplyDTO>> replyMap = replyDTOS.stream()
                .collect(Collectors.groupingBy(ReplyDTO::getParentId));

        // 组装评论数据，设置回复列表
        commentDTOs.forEach(item -> item.setReplyDTOs(replyMap.get(item.getId())));

        return new PageResultDTO<>(commentDTOs, commentCount);
    }

    /**
     * 根据评论ID获取回复列表
     *
     * 功能：
     * 1. 获取指定评论下的所有回复
     * 2. 回复按时间顺序排列
     * 3. 包含回复者和被回复者信息
     *
     * @param commentId 评论ID
     * @return List<ReplyDTO> 回复列表
     *         ReplyDTO包含：
     *         - 回复ID
     *         - 回复内容
     *         - 回复时间
     *         - 回复者信息
     *         - 被回复者信息
     */
    @Override
    public List<ReplyDTO> listRepliesByCommentId(Integer commentId) {
        // 将单个评论ID转换为列表，复用listReplies方法
        return commentMapper.listReplies(Collections.singletonList(commentId));
    }

    /**
     * 获取最新的六条评论
     *
     * 功能：
     * 1. 获取全站最新的六条评论
     * 2. 仅获取已审核的评论
     * 3. 评论按时间倒序排列
     *
     * 使用场景：
     * - 博客首页展示最新评论
     * - 侧边栏评论展示
     *
     * @return List<CommentDTO> 评论列表
     *         CommentDTO包含：
     *         - 评论ID
     *         - 评论内容
     *         - 评论时间
     *         - 评论者信息
     *         - 评论主题信息
     */
    @Override
    public List<CommentDTO> listTopSixComments() {
        return commentMapper.listTopSixComments();
    }

    /**
     * 获取后台评论列表（分页）
     * 使用异步方式统计评论总数，提高响应速度
     *
     * @param conditionVO 查询条件，包含关键词、评论类型等
     * @return PageResultDTO<CommentAdminDTO> 后台评论分页数据
     */
    @SneakyThrows
    @Override
    public PageResultDTO<CommentAdminDTO> listCommentsAdmin(ConditionVO conditionVO) {
        // 异步统计评论总数
        CompletableFuture<Integer> asyncCount = CompletableFuture.supplyAsync(() ->
                commentMapper.countComments(conditionVO));

        // 查询评论列表
        List<CommentAdminDTO> commentBackDTOList = commentMapper.listCommentsAdmin(
                PageUtil.getLimitCurrent(),
                PageUtil.getSize(),
                conditionVO);

        // 等待异步操作完成并返回结果
        return new PageResultDTO<>(commentBackDTOList, asyncCount.get());
    }


    /**
     * 批量更新评论审核状态
     *
     * @param reviewVO 审核信息，包含评论ID列表和审核状态
     */
    @Override
    public void updateCommentsReview(ReviewVO reviewVO) {
        // 构建评论对象列表
        List<Comment> comments = reviewVO.getIds().stream()
                .map(item -> Comment.builder()
                        .id(item)
                        .isReview(reviewVO.getIsReview())
                        .build())
                .collect(Collectors.toList());

        // 批量更新评论审核状态
        this.updateBatchById(comments);
    }

    /**
     * 校验评论数据的合法性
     *
     * 校验内容：
     * 1. 评论类型的合法性
     * 2. 文章和说说评论的主题ID校验
     * 3. 友链、关于我、留言板评论的主题ID校验
     * 4. 评论层级关系校验
     * 5. 回复用户的存在性校验
     *
     * @param commentVO 评论数据传输对象
     * @throws BizException 当校验失败时抛出异常，统一提示"参数校验异常"
     */
    public void checkCommentVO(CommentVO commentVO) {
        // 校验评论类型是否在预定义类型列表中
        if (!types.contains(commentVO.getType())) {
            throw new BizException("参数校验异常");
        }

        // 校验文章和说说评论
        if (Objects.requireNonNull(getCommentEnum(commentVO.getType())) == CommentTypeEnum.ARTICLE
                || Objects.requireNonNull(getCommentEnum(commentVO.getType())) == CommentTypeEnum.TALK) {
            // 主题ID必须存在
            if (Objects.isNull(commentVO.getTopicId())) {
                throw new BizException("参数校验异常");
            } else {
                // 文章评论校验
                if (Objects.requireNonNull(getCommentEnum(commentVO.getType())) == CommentTypeEnum.ARTICLE) {
                    // 检查文章是否存在
                    Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                            .select(Article::getId, Article::getUserId)
                            .eq(Article::getId, commentVO.getTopicId()));
                    if (Objects.isNull(article)) {
                        throw new BizException("参数校验异常");
                    }
                }
                // 说说评论校验
                if (Objects.requireNonNull(getCommentEnum(commentVO.getType())) == CommentTypeEnum.TALK) {
                    // 检查说说是否存在
                    Talk talk = talkMapper.selectOne(new LambdaQueryWrapper<Talk>()
                            .select(Talk::getId, Talk::getUserId)
                            .eq(Talk::getId, commentVO.getTopicId()));
                    if (Objects.isNull(talk)) {
                        throw new BizException("参数校验异常");
                    }
                }
            }
        }

        // 友链、关于我、留言板评论校验
        if (Objects.requireNonNull(getCommentEnum(commentVO.getType())) == CommentTypeEnum.LINK
                || Objects.requireNonNull(getCommentEnum(commentVO.getType())) == CommentTypeEnum.ABOUT
                || Objects.requireNonNull(getCommentEnum(commentVO.getType())) == CommentTypeEnum.MESSAGE) {
            // 这些类型的评论不应该有主题ID
            if (Objects.nonNull(commentVO.getTopicId())) {
                throw new BizException("参数校验异常");
            }
        }

        // 校验评论层级关系
        // 如果是一级评论（没有父评论）
        if (Objects.isNull(commentVO.getParentId())) {
            // 一级评论不应该有回复用户ID
            if (Objects.nonNull(commentVO.getReplyUserId())) {
                throw new BizException("参数校验异常");
            }
        }

        // 如果是回复评论（有父评论）
        if (Objects.nonNull(commentVO.getParentId())) {
            // 检查父评论是否存在
            Comment parentComment = commentMapper.selectOne(new LambdaQueryWrapper<Comment>()
                    .select(Comment::getId, Comment::getParentId, Comment::getType)
                    .eq(Comment::getId, commentVO.getParentId()));
            if (Objects.isNull(parentComment)) {
                throw new BizException("参数校验异常");
            }
            // 确保父评论是一级评论
            if (Objects.nonNull(parentComment.getParentId())) {
                throw new BizException("参数校验异常");
            }
            // 确保评论类型与父评论一致
            if (!commentVO.getType().equals(parentComment.getType())) {
                throw new BizException("参数校验异常");
            }
            // 回复评论必须指定被回复用户
            if (Objects.isNull(commentVO.getReplyUserId())) {
                throw new BizException("参数校验异常");
            } else {
                // 检查被回复用户是否存在
                UserInfo existUser = userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfo>()
                        .select(UserInfo::getId)
                        .eq(UserInfo::getId, commentVO.getReplyUserId()));
                if (Objects.isNull(existUser)) {
                    throw new BizException("参数校验异常");
                }
            }
        }
    }

    /**
     * 处理评论通知邮件发送
     *
     * 处理以下场景：
     * 1. 自己回复自己的评论（需要特殊处理）
     * 2. 博主发布一级评论（不需要通知）
     * 3. 评论中@其他用户（需要发送@提醒）
     * 4. 回复他人评论（需要通知原评论作者）
     *
     * @param comment 评论实体，包含评论的完整信息
     * @param fromNickname 评论者的昵称
     */
    private void notice(Comment comment, String fromNickname) {
        // 场景1：处理自己回复自己的评论
        if (comment.getUserId().equals(comment.getReplyUserId())) {
            // 如果存在父评论，检查是否需要通知
            if (Objects.nonNull(comment.getParentId())) {
                Comment parentComment = commentMapper.selectById(comment.getParentId());
                // 如果父评论也是自己的，不需要通知
                if (parentComment.getUserId().equals(comment.getUserId())) {
                    return;
                }
            }
        }

        // 场景2：博主发布一级评论时不发送通知
        if (comment.getUserId().equals(CommonConstant.BLOGGER_ID)
                && Objects.isNull(comment.getParentId())) {
            return;
        }

        // 场景3：处理评论中@用户的情况
        if (Objects.nonNull(comment.getParentId())) {
            Comment parentComment = commentMapper.selectById(comment.getParentId());
            // 如果回复的用户不是父评论作者且不是自己，说明是@了其他用户
            if (!comment.getReplyUserId().equals(parentComment.getUserId())
                    && !comment.getReplyUserId().equals(comment.getUserId())) {
                // 获取相关用户信息
                UserInfo userInfo = userInfoMapper.selectById(comment.getUserId());
                UserInfo replyUserinfo = userInfoMapper.selectById(comment.getReplyUserId());

                // 构建@提醒邮件内容
                Map<String, Object> map = new HashMap<>();
                String topicId = Objects.nonNull(comment.getTopicId()) ?
                        comment.getTopicId().toString() : "";
                String url = websiteUrl + CommentTypeEnum.getCommentPath(comment.getType())
                        + topicId;

                // 设置邮件内容，包含评论链接
                map.put("content", userInfo.getNickname() + "在"
                        + Objects.requireNonNull(getCommentEnum(comment.getType())).getDesc()
                        + "的评论区@了你，"
                        + "<a style=\"text-decoration:none;color:#12addb\" href=\""
                        + url + "\">点击查看</a>");

                // 构建并发送@提醒邮件
                EmailDTO emailDTO = EmailDTO.builder()
                        .email(replyUserinfo.getEmail())
                        .subject(CommonConstant.MENTION_REMIND)
                        .template("common.html")
                        .commentMap(map)
                        .build();
                rabbitTemplate.convertAndSend(RabbitMQConstant.EMAIL_EXCHANGE, "*",
                        new Message(JSON.toJSONBytes(emailDTO), new MessageProperties()));
            }

            // 如果是回复自己的评论，不需要额外通知
            if (comment.getUserId().equals(parentComment.getUserId())) {
                return;
            }
        }

        // 场景4：处理评论通知
        // 确定通知接收者和评论标题
        String title;
        Integer userId = CommonConstant.BLOGGER_ID;
        String topicId = Objects.nonNull(comment.getTopicId()) ?
                comment.getTopicId().toString() : "";

        // 确定通知接收者
        if (Objects.nonNull(comment.getReplyUserId())) {
            // 如果是回复评论，通知被回复的用户
            userId = comment.getReplyUserId();
        } else {
            // 如果是一级评论，根据评论类型通知相应的作者
            switch (Objects.requireNonNull(getCommentEnum(comment.getType()))) {
                case ARTICLE:
                    // 文章评论通知文章作者
                    userId = articleMapper.selectById(comment.getTopicId()).getUserId();
                    break;
                case TALK:
                    // 说说评论通知说说作者
                    userId = talkMapper.selectById(comment.getTopicId()).getUserId();
                default:
                    break;
            }
        }

        // 设置评论标题
        if (Objects.requireNonNull(getCommentEnum(comment.getType()))
                .equals(CommentTypeEnum.ARTICLE)) {
            // 文章评论使用文章标题
            title = articleMapper.selectById(comment.getTopicId()).getArticleTitle();
        } else {
            // 其他类型使用评论类型描述
            title = Objects.requireNonNull(getCommentEnum(comment.getType())).getDesc();
        }

        // 获取接收者信息并发送通知邮件
        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (StringUtils.isNotBlank(userInfo.getEmail())) {
            // 构建并发送评论通知邮件
            EmailDTO emailDTO = getEmailDTO(comment, userInfo, fromNickname, topicId, title);
            rabbitTemplate.convertAndSend(RabbitMQConstant.EMAIL_EXCHANGE, "*",
                    new Message(JSON.toJSONBytes(emailDTO), new MessageProperties()));
        }
    }

    /**
     * 构建评论邮件通知的数据传输对象
     *
     * 根据不同场景构建不同的邮件内容：
     * 1. 评论已审核：
     *    - 一级评论：使用 owner.html 模板
     *    - 回复评论：使用 user.html 模板
     * 2. 评论待审核：使用 common.html 模板通知管理员
     *
     * @param comment 评论信息
     * @param userInfo 接收通知的用户信息
     * @param fromNickname 评论者昵称
     * @param topicId 主题ID
     * @param title 主题标题
     * @return EmailDTO 邮件数据传输对象
     */
    private EmailDTO getEmailDTO(Comment comment, UserInfo userInfo, String fromNickname,
                                 String topicId, String title) {
        EmailDTO emailDTO = new EmailDTO();
        Map<String, Object> map = new HashMap<>();

        // 处理已审核的评论通知
        if (comment.getIsReview().equals(CommonConstant.TRUE)) {
            // 构建评论链接
            String url = websiteUrl + CommentTypeEnum.getCommentPath(comment.getType()) + topicId;

            // 处理一级评论通知
            if (Objects.isNull(comment.getParentId())) {
                // 设置一级评论邮件信息
                emailDTO.setEmail(userInfo.getEmail());
                emailDTO.setSubject(CommonConstant.COMMENT_REMIND);
                emailDTO.setTemplate("owner.html");  // 使用文章作者通知模板

                // 格式化评论时间
                String createTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        .format(comment.getCreateTime());

                // 设置模板变量
                map.put("time", createTime);        // 评论时间
                map.put("url", url);               // 评论链接
                map.put("title", title);           // 文章标题
                map.put("nickname", fromNickname); // 评论者昵称
                map.put("content", comment.getCommentContent()); // 评论内容
            } else {
                // 处理回复评论通知
                // 获取父评论信息
                Comment parentComment = commentMapper.selectOne(new LambdaQueryWrapper<Comment>()
                        .select(Comment::getUserId, Comment::getCommentContent, Comment::getCreateTime)
                        .eq(Comment::getId, comment.getParentId()));

                // 如果通知接收者不是父评论作者，更新接收者信息
                if (!userInfo.getId().equals(parentComment.getUserId())) {
                    userInfo = userInfoMapper.selectById(parentComment.getUserId());
                }

                // 设置回复评论邮件信息
                emailDTO.setEmail(userInfo.getEmail());
                emailDTO.setSubject(CommonConstant.COMMENT_REMIND);
                emailDTO.setTemplate("user.html");  // 使用用户回复通知模板

                // 设置基本模板变量
                map.put("url", url);
                map.put("title", title);
                String createTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        .format(parentComment.getCreateTime());
                map.put("time", createTime);
                map.put("toUser", userInfo.getNickname());    // 接收通知的用户
                map.put("fromUser", fromNickname);           // 评论者
                map.put("parentComment", parentComment.getCommentContent()); // 原评论内容

                // 处理@用户的情况
                if (!comment.getReplyUserId().equals(parentComment.getUserId())) {
                    // 获取被@用户信息
                    UserInfo mentionUserInfo = userInfoMapper.selectById(comment.getReplyUserId());
                    // 如果被@用户有个人网站，添加链接
                    if (Objects.nonNull(mentionUserInfo.getWebsite())) {
                        map.put("replyComment", "<a style=\"text-decoration:none;color:#12addb\" href=\""
                                + mentionUserInfo.getWebsite()
                                + "\">@" + mentionUserInfo.getNickname() + " " + "</a>"
                                + parentComment.getCommentContent());
                    } else {
                        map.put("replyComment", "@" + mentionUserInfo.getNickname() + " "
                                + parentComment.getCommentContent());
                    }
                } else {
                    // 直接回复，不是@用户
                    map.put("replyComment", comment.getCommentContent());
                }
            }
        } else {
            // 处理待审核的评论通知
            // 获取管理员邮箱并发送审核提醒
            String adminEmail = userInfoMapper.selectById(CommonConstant.BLOGGER_ID).getEmail();
            emailDTO.setEmail(adminEmail);
            emailDTO.setSubject(CommonConstant.CHECK_REMIND);
            emailDTO.setTemplate("common.html");  // 使用通用提醒模板
            map.put("content", "您收到了一条新的回复，请前往后台管理页面审核");
        }

        // 设置邮件模板变量
        emailDTO.setCommentMap(map);
        return emailDTO;
    }

}
