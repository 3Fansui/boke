package com.boke.db.service.impl;

import com.alibaba.fastjson.JSON;
import com.boke.constant.CommonConstant;
import com.boke.constant.RedisConstant;
import com.boke.db.entity.About;
import com.boke.db.entity.Article;
import com.boke.db.entity.Comment;
import com.boke.db.entity.WebsiteConfig;
import com.boke.db.mapper.*;
import com.boke.db.mapper.*;
import com.boke.db.service.bokeInfoService;
import com.boke.db.service.RedisService;
import com.boke.db.service.UniqueViewService;
import com.boke.model.dto.*;
import com.boke.util.BeanCopyUtil;
import com.boke.util.IpUtil;
import com.boke.model.vo.AboutVO;
import com.boke.model.vo.WebsiteConfigVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 博客信息服务实现类
 *
 * 该服务类主要处理:
 * 1. 网站访问统计与报告
 * 2. 博客首页信息获取
 * 3. 后台管理统计信息
 * 4. 网站配置的更新与获取
 * 5. "关于我"页面的更新与获取
 *
 * @author boke
 * @since 1.0.0
 */
@Service
public class bokeInfoServiceImpl implements bokeInfoService {

    @Autowired
    private WebsiteConfigMapper websiteConfigMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private TalkMapper talkMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private AboutMapper aboutMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private UniqueViewService uniqueViewService;

    @Autowired
    private HttpServletRequest request;


    /**
     * 上报访客信息并统计
     * 该方法用于记录和统计网站访客信息，包括：
     * 1. 访客IP地址
     * 2. 浏览器类型
     * 3. 操作系统信息
     * 4. 访客地理位置
     * 5. 访问量统计
     *
     * 使用Redis进行数据存储和统计：
     * - 使用Set结构存储唯一访客
     * - 使用Hash结构存储地区访问统计
     * - 使用String结构存储总访问量
     */
    @Override
    public void report() {
        // 获取访客IP地址
        String ipAddress = IpUtil.getIpAddress(request);

        // 获取用户代理信息（浏览器、操作系统等）
        UserAgent userAgent = IpUtil.getUserAgent(request);
        Browser browser = userAgent.getBrowser();
        OperatingSystem operatingSystem = userAgent.getOperatingSystem();

        // 生成访客唯一标识（IP + 浏览器 + 操作系统）
        String uuid = ipAddress + browser.getName() + operatingSystem.getName();
        // 对唯一标识进行MD5加密
        String md5 = DigestUtils.md5DigestAsHex(uuid.getBytes());

        // 检查是否为新的唯一访客
        if (!redisService.sIsMember(RedisConstant.UNIQUE_VISITOR, md5)) {
            // 获取IP来源地址
            String ipSource = IpUtil.getIpSource(ipAddress);

            // 统计地区访问量
            if (StringUtils.isNotBlank(ipSource)) {
                // 提取省份信息并增加计数
                String ipProvince = IpUtil.getIpProvince(ipSource);
                redisService.hIncr(RedisConstant.VISITOR_AREA, ipProvince, 1L);
            } else {
                // 无法获取地址信息时，计入未知地区
                redisService.hIncr(RedisConstant.VISITOR_AREA, CommonConstant.UNKNOWN, 1L);
            }

            // 增加网站总访问量计数
            redisService.incr(RedisConstant.BLOG_VIEWS_COUNT, 1);

            // 将访客标识添加到唯一访客集合
            redisService.sAdd(RedisConstant.UNIQUE_VISITOR, md5);
        }
    }


    /**
     * 获取博客首页信息
     * 使用CompletableFuture实现异步并行查询，提高响应速度
     * 统计信息包括：
     * - 文章数量
     * - 分类数量
     * - 标签数量
     * - 说说数量
     * - 网站配置信息
     * - 网站访问量
     *
     * @return bokeHomeInfoDTO 首页信息数据传输对象
     * @throws InterruptedException 线程中断异常
     */
    @SneakyThrows  // Lombok注解，自动处理受检异常
    @Override
    public bokeHomeInfoDTO getAuroraHomeInfo() {
        // 异步查询文章数量（未删除的文章）
        CompletableFuture<Integer> asyncArticleCount = CompletableFuture.supplyAsync(() ->
                articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                        .eq(Article::getIsDelete, CommonConstant.FALSE))
        );

        // 异步查询分类数量
        CompletableFuture<Integer> asyncCategoryCount = CompletableFuture.supplyAsync(() ->
                categoryMapper.selectCount(null)
        );

        // 异步查询标签数量
        CompletableFuture<Integer> asyncTagCount = CompletableFuture.supplyAsync(() ->
                tagMapper.selectCount(null)
        );

        // 异步查询说说数量
        CompletableFuture<Integer> asyncTalkCount = CompletableFuture.supplyAsync(() ->
                talkMapper.selectCount(null)
        );

        // 异步获取网站配置信息
        CompletableFuture<WebsiteConfigDTO> asyncWebsiteConfig = CompletableFuture.supplyAsync(
                this::getWebsiteConfig
        );

        // 异步获取网站访问量（从Redis缓存获取）
        CompletableFuture<Integer> asyncViewCount = CompletableFuture.supplyAsync(() -> {
            Object count = redisService.get(RedisConstant.BLOG_VIEWS_COUNT);
            return Integer.parseInt(Optional.ofNullable(count).orElse(0).toString());
        });

        // 使用Builder模式构建并返回首页信息DTO
        // get()方法会等待异步操作完成并获取结果
        return bokeHomeInfoDTO.builder()
                .articleCount(asyncArticleCount.get())       // 获取文章数量
                .categoryCount(asyncCategoryCount.get())     // 获取分类数量
                .tagCount(asyncTagCount.get())              // 获取标签数量
                .talkCount(asyncTalkCount.get())            // 获取说说数量
                .websiteConfigDTO(asyncWebsiteConfig.get())  // 获取网站配置
                .viewCount(asyncViewCount.get())            // 获取访问量
                .build();
    }


    /**
     * 获取博客后台管理统计信息
     * 该方法汇总了博客系统的各项统计数据，包括：
     * - 网站访问量
     * - 留言数量
     * - 用户数量
     * - 文章数量
     * - 独立访客统计
     * - 文章统计信息
     * - 分类统计
     * - 标签统计
     * - 热门文章排行
     *
     * @return bokeAdminInfoDTO 后台统计信息数据传输对象
     */
    @Override
    public bokeAdminInfoDTO getAuroraAdminInfo() {
        // 从Redis获取网站总访问量，如果为空则默认为0
        Object count = redisService.get(RedisConstant.BLOG_VIEWS_COUNT);
        Integer viewsCount = Integer.parseInt(Optional.ofNullable(count).orElse(0).toString());

        // 统计留言数量（type=2 表示留言类型的评论）
        Integer messageCount = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getType, 2));

        // 统计用户总数
        Integer userCount = userInfoMapper.selectCount(null);

        // 统计未删除的文章总数
        Integer articleCount = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getIsDelete, CommonConstant.FALSE));

        // 获取独立访客统计数据
        List<UniqueViewDTO> uniqueViews = uniqueViewService.listUniqueViews();

        // 获取文章统计信息（如文章发布趋势等）
        List<ArticleStatisticsDTO> articleStatisticsDTOs = articleMapper.listArticleStatistics();

        // 获取分类统计信息
        List<CategoryDTO> categoryDTOs = categoryMapper.listCategories();

        // 获取标签统计信息并转换为DTO
        List<TagDTO> tagDTOs = BeanCopyUtil.copyList(tagMapper.selectList(null), TagDTO.class);

        // 从Redis获取文章访问量排行榜数据（获取前5篇文章）
        Map<Object, Double> articleMap = redisService.zReverseRangeWithScore(
                RedisConstant.ARTICLE_VIEWS_COUNT, 0, 4);

        // 构建后台信息DTO对象
        bokeAdminInfoDTO auroraAdminInfoDTO = bokeAdminInfoDTO.builder()
                .articleStatisticsDTOs(articleStatisticsDTOs)  // 文章统计
                .tagDTOs(tagDTOs)                             // 标签统计
                .viewsCount(viewsCount)                       // 访问量
                .messageCount(messageCount)                   // 留言数
                .userCount(userCount)                         // 用户数
                .articleCount(articleCount)                   // 文章数
                .categoryDTOs(categoryDTOs)                   // 分类统计
                .uniqueViewDTOs(uniqueViews)                 // 独立访客统计
                .build();

        // 如果存在文章访问量数据，则获取文章排行榜
        if (CollectionUtils.isNotEmpty(articleMap)) {
            List<ArticleRankDTO> articleRankDTOList = listArticleRank(articleMap);
            auroraAdminInfoDTO.setArticleRankDTOs(articleRankDTOList);
        }

        return auroraAdminInfoDTO;
    }

    /**
     * 更新网站配置信息
     * 1. 将配置对象转换为JSON字符串存储
     * 2. 更新数据库中的配置信息
     * 3. 删除Redis缓存以便下次获取最新配置
     *
     * @param websiteConfigVO 网站配置值对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWebsiteConfig(WebsiteConfigVO websiteConfigVO) {
        // 构建配置实体对象
        WebsiteConfig websiteConfig = WebsiteConfig.builder()
                .id(CommonConstant.DEFAULT_CONFIG_ID)  // 使用默认配置ID
                .config(JSON.toJSONString(websiteConfigVO))  // 将配置对象转为JSON字符串
                .build();
        // 更新数据库
        websiteConfigMapper.updateById(websiteConfig);
        // 删除Redis缓存
        redisService.del(RedisConstant.WEBSITE_CONFIG);
    }

    /**
     * 获取网站配置信息
     * 1. 优先从Redis缓存获取
     * 2. 缓存未命中则从数据库读取
     * 3. 将数据库结果存入Redis缓存
     *
     * @return WebsiteConfigDTO 网站配置数据传输对象
     */
    @Override
    public WebsiteConfigDTO getWebsiteConfig() {
        WebsiteConfigDTO websiteConfigDTO;
        // 尝试从Redis获取配置
        Object websiteConfig = redisService.get(RedisConstant.WEBSITE_CONFIG);
        if (Objects.nonNull(websiteConfig)) {
            // Redis中存在，直接解析返回
            websiteConfigDTO = JSON.parseObject(websiteConfig.toString(), WebsiteConfigDTO.class);
        } else {
            // Redis中不存在，从数据库读取
            String config = websiteConfigMapper.selectById(CommonConstant.DEFAULT_CONFIG_ID).getConfig();
            websiteConfigDTO = JSON.parseObject(config, WebsiteConfigDTO.class);
            // 将配置信息存入Redis
            redisService.set(RedisConstant.WEBSITE_CONFIG, config);
        }
        return websiteConfigDTO;
    }

    /**
     * 更新"关于我"页面信息
     * 1. 将AboutVO对象转换为JSON存储
     * 2. 更新数据库
     * 3. 清除Redis缓存
     *
     * @param aboutVO "关于我"页面值对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAbout(AboutVO aboutVO) {
        // 构建About实体
        About about = About.builder()
                .id(CommonConstant.DEFAULT_ABOUT_ID)  // 使用默认ID
                .content(JSON.toJSONString(aboutVO))  // 转换为JSON存储
                .build();
        // 更新数据库
        aboutMapper.updateById(about);
        // 删除Redis缓存
        redisService.del(RedisConstant.ABOUT);
    }

    /**
     * 获取"关于我"页面信息
     * 1. 优先从Redis缓存获取
     * 2. 缓存未命中则从数据库读取
     * 3. 将数据库结果存入Redis缓存
     *
     * @return AboutDTO "关于我"页面数据传输对象
     */
    @Override
    public AboutDTO getAbout() {
        AboutDTO aboutDTO;
        // 尝试从Redis获取
        Object about = redisService.get(RedisConstant.ABOUT);
        if (Objects.nonNull(about)) {
            // Redis中存在，直接解析返回
            aboutDTO = JSON.parseObject(about.toString(), AboutDTO.class);
        } else {
            // Redis中不存在，从数据库读取
            String content = aboutMapper.selectById(CommonConstant.DEFAULT_ABOUT_ID).getContent();
            aboutDTO = JSON.parseObject(content, AboutDTO.class);
            // 将内容存入Redis
            redisService.set(RedisConstant.ABOUT, content);
        }
        return aboutDTO;
    }

    /**
     * 获取文章访问量排行榜
     * 1. 将Redis中的文章ID转换为List
     * 2. 根据ID批量查询文章标题
     * 3. 组装文章标题和访问量信息
     * 4. 按访问量降序排序
     *
     * @param articleMap Redis中的文章访问量Map(key:文章ID, value:访问量)
     * @return List<ArticleRankDTO> 文章排行榜列表
     */
    private List<ArticleRankDTO> listArticleRank(Map<Object, Double> articleMap) {
        // 提取文章ID列表
        List<Integer> articleIds = new ArrayList<>(articleMap.size());
        articleMap.forEach((key, value) -> articleIds.add((Integer) key));

        // 批量查询文章信息并转换为DTO
        return articleMapper.selectList(new LambdaQueryWrapper<Article>()
                        .select(Article::getId, Article::getArticleTitle)  // 只查询需要的字段
                        .in(Article::getId, articleIds))  // 按ID批量查询
                .stream().map(article -> ArticleRankDTO.builder()
                        .articleTitle(article.getArticleTitle())  // 设置文章标题
                        .viewsCount(articleMap.get(article.getId()).intValue())  // 设置访问量
                        .build())
                .sorted(Comparator.comparingInt(ArticleRankDTO::getViewsCount).reversed())  // 按访问量降序排序
                .collect(Collectors.toList());
    }

}
