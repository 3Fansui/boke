package com.boke.db.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boke.constant.RabbitMQConstant;
import com.boke.constant.RedisConstant;
import com.boke.db.mapper.ArticleMapper;
import com.boke.db.mapper.ArticleTagMapper;
import com.boke.db.mapper.CategoryMapper;
import com.boke.db.mapper.TagMapper;
import com.boke.db.service.ArticleService;
import com.boke.db.entity.Article;
import com.boke.db.entity.ArticleTag;
import com.boke.db.entity.Category;
import com.boke.db.entity.Tag;
import com.boke.enums.ArticleStatusEnum;
import com.boke.enums.FileExtEnum;
import com.boke.enums.FilePathEnum;
import com.boke.enums.StatusCodeEnum;
import com.boke.exception.BizException;
import com.boke.db.service.ArticleTagService;
import com.boke.db.service.RedisService;
import com.boke.db.service.TagService;
import com.boke.model.dto.*;
import com.boke.model.vo.*;
import com.boke.strategy.context.SearchStrategyContext;
import com.boke.strategy.context.UploadStrategyContext;
import com.boke.util.BeanCopyUtil;
import com.boke.util.PageUtil;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boke.util.UserUtil;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private TagService tagService;

    @Autowired
    private ArticleTagService articleTagService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UploadStrategyContext uploadStrategyContext;

    @Autowired
    private SearchStrategyContext searchStrategyContext;

    /**
     * 该方法获取置顶和精选文章列表。
     * 它首先从数据库中获取所有置顶和精选文章，然后处理它们以确定最终的置顶文章和精选文章列表。
     *
     * @return 包含置顶文章和精选文章列表的 TopAndFeaturedArticlesDTO 对象。
     */
    @SneakyThrows
    @Override
    public TopAndFeaturedArticlesDTO listTopAndFeaturedArticles() {
        // 从数据库中获取置顶和精选文章列表
        List<ArticleCardDTO> articleCardDTOs = articleMapper.listTopAndFeaturedArticles();

        // 如果列表为空，则返回一个新的 TopAndFeaturedArticlesDTO 对象
        if (articleCardDTOs.isEmpty()) {
            return new TopAndFeaturedArticlesDTO();
        } else if (articleCardDTOs.size() > 3) {
            // 如果列表中有超过3篇文章，则只保留前3篇文章
            articleCardDTOs = articleCardDTOs.subList(0, 3);
        }

        // 创建一个新的 TopAndFeaturedArticlesDTO 对象
        TopAndFeaturedArticlesDTO topAndFeaturedArticlesDTO = new TopAndFeaturedArticlesDTO();

        // 将第一篇文章设置为置顶文章
        topAndFeaturedArticlesDTO.setTopArticle(articleCardDTOs.get(0));

        // 从列表中移除第一篇文章
        articleCardDTOs.remove(0);

        // 将剩余的文章设置为精选文章
        topAndFeaturedArticlesDTO.setFeaturedArticles(articleCardDTOs);

        // 返回包含置顶和精选文章的 TopAndFeaturedArticlesDTO 对象
        return topAndFeaturedArticlesDTO;
    }


    /**
     * 该方法获取所有文章列表，并返回包含文章信息和总数的分页结果。
     * 它首先通过 lambdaQuery 创建查询条件，然后进行分页查询，按条件获取文章列表。
     * 最后将文章对象转换为 DTO 对象列表，并返回分页结果对象。
     *
     * @return 包含文章信息和总数的 PageResultDTO 对象。
     */
    @SneakyThrows
    @Override
    public PageResultDTO<ArticleCardDTO> listArticles() {

        // 使用lambda查询
        Page<Article> articlePage = this.lambdaQuery()
                .eq(Article::getIsDelete, 0)          // 查询未删除的文章
                .in(Article::getStatus, 1, 2)         // 查询状态为1或2的文章
                .orderByDesc(Article::getIsTop)       // 置顶文章优先
                .orderByDesc(Article::getCreateTime)  // 按创建时间倒序
                .page(new Page<>(PageUtil.getCurrent(), PageUtil.getSize())); // 分页查询

        // 将Article转换为ArticleCardDTO
        List<ArticleCardDTO> articleCardDTOs = BeanCopyUtil.copyList(articlePage.getRecords(), ArticleCardDTO.class);

        // 返回分页结果
        return new PageResultDTO<>(articleCardDTOs, (int) articlePage.getTotal());
    }



    /**
     * 该方法根据分类ID获取文章列表，并返回包含文章信息和总数的分页结果。
     * 它首先通过 LambdaQueryWrapper 创建查询条件，然后异步获取文章总数。
     * 最后获取文章列表并将其封装在分页结果对象中返回。
     *
     * @param categoryId 分类ID
     * @return 包含文章信息和总数的 PageResultDTO 对象
     */
    @SneakyThrows
    @Override
    public PageResultDTO<ArticleCardDTO> listArticlesByCategoryId(Integer categoryId) {
        // 使用 LambdaQueryWrapper 创建查询条件，用于查询指定分类的文章
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getCategoryId, categoryId);

        // 异步获取符合条件的文章总数
        CompletableFuture<Integer> asyncCount = CompletableFuture.supplyAsync(() -> articleMapper.selectCount(queryWrapper));

        // 获取分页参数并查询文章列表
        List<ArticleCardDTO> articles = articleMapper.getArticlesByCategoryId(PageUtil.getLimitCurrent(), PageUtil.getSize(), categoryId);

        // 返回包含文章列表和总数的分页结果对象
        return new PageResultDTO<>(articles, asyncCount.get());
    }


    /**
     * 该方法根据文章ID获取文章详情，并返回包含相关信息的 ArticleDTO 对象。
     * 它首先检查文章是否存在，并根据文章状态进行权限检查。
     * 然后异步获取文章详情、前一篇文章和后一篇文章，并更新文章的浏览次数。
     * 最后返回包含文章详情及前后文章信息的 ArticleDTO 对象。
     *
     * @param articleId 文章ID
     * @return 包含文章详情及前后文章信息的 ArticleDTO 对象
     * @throws BizException 当权限检查失败时抛出异常
     */
    @SneakyThrows
    @Override
    public ArticleDTO getArticleById(Integer articleId) {
        // 检查文章是否存在
        Article articleForCheck = articleMapper.selectOne(new LambdaQueryWrapper<Article>().eq(Article::getId, articleId));
        if (Objects.isNull(articleForCheck)) {
            return null;
        }

        // 如果文章状态为2，进行权限检查
        if (articleForCheck.getStatus().equals(2)) {
            Boolean isAccess;
            try {
                isAccess = redisService.sIsMember(RedisConstant.ARTICLE_ACCESS + UserUtil.getUserDetailsDTO().getId(), articleId);
            } catch (Exception exception) {
                throw new BizException(StatusCodeEnum.ARTICLE_ACCESS_FAIL);
            }
            if (isAccess.equals(false)) {
                throw new BizException(StatusCodeEnum.ARTICLE_ACCESS_FAIL);
            }
        }

        // 更新文章浏览次数
        updateArticleViewsCount(articleId);

        // 异步获取文章详情
        CompletableFuture<ArticleDTO> asyncArticle = CompletableFuture.supplyAsync(() -> articleMapper.getArticleById(articleId));

        // 异步获取前一篇文章
        CompletableFuture<ArticleCardDTO> asyncPreArticle = CompletableFuture.supplyAsync(() -> {
            ArticleCardDTO preArticle = articleMapper.getPreArticleById(articleId);
            if (Objects.isNull(preArticle)) {
                preArticle = articleMapper.getLastArticle();
            }
            return preArticle;
        });

        // 异步获取后一篇文章
        CompletableFuture<ArticleCardDTO> asyncNextArticle = CompletableFuture.supplyAsync(() -> {
            ArticleCardDTO nextArticle = articleMapper.getNextArticleById(articleId);
            if (Objects.isNull(nextArticle)) {
                nextArticle = articleMapper.getFirstArticle();
            }
            return nextArticle;
        });

        // 获取文章详情
        ArticleDTO article = asyncArticle.get();
        if (Objects.isNull(article)) {
            return null;
        }

        // 获取文章浏览次数
        Double score = redisService.zScore(RedisConstant.ARTICLE_VIEWS_COUNT, articleId);
        if (Objects.nonNull(score)) {
            article.setViewCount(score.intValue());
        }

        // 设置前一篇文章和后一篇文章
        article.setPreArticleCard(asyncPreArticle.get());
        article.setNextArticleCard(asyncNextArticle.get());

        // 返回包含文章详情及前后文章信息的 ArticleDTO 对象
        return article;
    }


    /**
     * 校验文章访问密码
     * 根据文章ID获取文章详情，并校验用户提供的访问密码是否正确。
     * 如果密码正确，记录用户的访问权限；否则，抛出密码错误的异常。
     *
     * @param articlePasswordVO 包含文章ID和访问密码的VO对象
     * @throws BizException 当文章不存在或密码错误时抛出异常
     */
    @Override
    public void accessArticle(ArticlePasswordVO articlePasswordVO) {
        // 根据文章ID查询文章
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>().eq(Article::getId, articlePasswordVO.getArticleId()));

        // 如果文章不存在，抛出异常
        if (Objects.isNull(article)) {
            throw new BizException("文章不存在");
        }

        // 如果文章密码匹配，将用户ID和文章ID存入Redis以记录访问权限
        if (article.getPassword().equals(articlePasswordVO.getArticlePassword())) {
            redisService.sAdd(RedisConstant.ARTICLE_ACCESS + UserUtil.getUserDetailsDTO().getId(), articlePasswordVO.getArticleId());
        } else {
            // 如果密码不匹配，抛出异常
            throw new BizException("密码错误");
        }
    }


    /**
     * 根据标签ID获取文章列表，并返回包含文章信息和总数的分页结果。
     * 首先通过 LambdaQueryWrapper 创建查询条件，然后异步获取文章总数。
     * 最后获取文章列表，并将其封装在分页结果对象中返回。
     *
     * @param tagId 标签ID
     * @return 包含文章信息和总数的 PageResultDTO 对象
     */
    @SneakyThrows
    @Override
    public PageResultDTO<ArticleCardDTO> listArticlesByTagId(Integer tagId) {
        // 使用 LambdaQueryWrapper 创建查询条件，用于查询指定标签的文章
        LambdaQueryWrapper<ArticleTag> queryWrapper = new LambdaQueryWrapper<ArticleTag>()
                .eq(ArticleTag::getTagId, tagId);

        // 异步获取符合条件的文章总数
        CompletableFuture<Integer> asyncCount = CompletableFuture.supplyAsync(() -> articleTagMapper.selectCount(queryWrapper));

        // 获取分页参数并查询文章列表
        List<ArticleCardDTO> articles = articleMapper.listArticlesByTagId(PageUtil.getLimitCurrent(), PageUtil.getSize(), tagId);

        // 返回包含文章列表和总数的分页结果对象
        return new PageResultDTO<>(articles, asyncCount.get());
    }


    /**
     * 获取所有文章归档
     * 该方法获取所有未删除且状态为1的文章，并按月归档返回包含文章信息和总数的分页结果。
     * 首先通过 LambdaQueryWrapper 创建查询条件，然后异步获取文章总数。
     * 最后获取文章列表，按月份归档并封装在分页结果对象中返回。
     *
     * @return 包含按月份归档的文章信息和总数的 PageResultDTO 对象
     */
    @SneakyThrows
    @Override
    public PageResultDTO<ArchiveDTO> listArchives() {
        // 使用 LambdaQueryWrapper 创建查询条件，用于查询未删除且状态为1的文章
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getIsDelete, 0)
                .eq(Article::getStatus, 1);

        // 异步获取符合条件的文章总数
        CompletableFuture<Integer> asyncCount = CompletableFuture.supplyAsync(() -> articleMapper.selectCount(queryWrapper));

        // 获取分页参数并查询文章列表
        List<ArticleCardDTO> articles = articleMapper.listArchives(PageUtil.getLimitCurrent(), PageUtil.getSize());

        // 使用 HashMap 按年月归档文章
        HashMap<String, List<ArticleCardDTO>> map = new HashMap<>();
        for (ArticleCardDTO article : articles) {
            LocalDateTime createTime = article.getCreateTime();
            int month = createTime.getMonth().getValue();
            int year = createTime.getYear();
            String key = year + "-" + month;
            if (Objects.isNull(map.get(key))) {
                List<ArticleCardDTO> articleCardDTOS = new ArrayList<>();
                articleCardDTOS.add(article);
                map.put(key, articleCardDTOS);
            } else {
                map.get(key).add(article);
            }
        }

        // 将归档信息转换为 ArchiveDTO 列表
        List<ArchiveDTO> archiveDTOs = new ArrayList<>();
        map.forEach((key, value) -> archiveDTOs.add(ArchiveDTO.builder().Time(key).articles(value).build()));

        // 对归档信息按年月进行排序
        archiveDTOs.sort((o1, o2) -> {
            String[] o1s = o1.getTime().split("-");
            String[] o2s = o2.getTime().split("-");
            int o1Year = Integer.parseInt(o1s[0]);
            int o1Month = Integer.parseInt(o1s[1]);
            int o2Year = Integer.parseInt(o2s[0]);
            int o2Month = Integer.parseInt(o2s[1]);
            if (o1Year > o2Year) {
                return -1;
            } else if (o1Year < o2Year) {
                return 1;
            } else {
                return Integer.compare(o2Month, o1Month);
            }
        });

        // 返回包含按月份归档的文章信息和总数的分页结果对象
        return new PageResultDTO<>(archiveDTOs, asyncCount.get());
    }


    /**
     * 获取后台文章
     * 该方法获取后台管理的文章列表，并返回包含文章信息和总数的分页结果。
     * 首先异步获取文章总数，然后获取文章列表并更新文章的浏览次数。
     * 最后将结果封装在分页结果对象中返回。
     *
     * @param conditionVO 包含查询条件的 VO 对象
     * @return 包含文章信息和总数的 PageResultDTO 对象
     */
    @SneakyThrows
    @Override
    public PageResultDTO<ArticleAdminDTO> listArticlesAdmin(ConditionVO conditionVO) {
        // 异步获取符合条件的文章总数
        CompletableFuture<Integer> asyncCount = CompletableFuture.supplyAsync(() -> articleMapper.countArticleAdmins(conditionVO));

        // 获取分页参数并查询文章列表
        List<ArticleAdminDTO> articleAdminDTOs = articleMapper.listArticlesAdmin(PageUtil.getLimitCurrent(), PageUtil.getSize(), conditionVO);

        // 从Redis中获取所有文章的浏览次数
        Map<Object, Double> viewsCountMap = redisService.zAllScore(RedisConstant.ARTICLE_VIEWS_COUNT);

        // 更新每篇文章的浏览次数
        articleAdminDTOs.forEach(item -> {
            Double viewsCount = viewsCountMap.get(item.getId());
            if (Objects.nonNull(viewsCount)) {
                item.setViewsCount(viewsCount.intValue());
            }
        });

        // 返回包含文章列表和总数的分页结果对象
        return new PageResultDTO<>(articleAdminDTOs, asyncCount.get());
    }

    /**
     * 保存或修改文章
     * 该方法根据提供的文章信息保存或更新文章。
     * 首先保存文章分类信息，然后复制文章对象属性并设置相关信息。
     * 接着保存或更新文章，并保存文章标签信息。
     * 如果文章状态为发布状态，将文章ID发送到消息队列。
     *
     * @param articleVO 包含文章信息的 VO 对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateArticle(ArticleVO articleVO) {
        // 保存文章分类信息
        Category category = saveArticleCategory(articleVO);

        // 复制文章对象属性
        Article article = BeanCopyUtil.copyObject(articleVO, Article.class);

        // 设置分类ID（如果分类不为空）
        if (Objects.nonNull(category)) {
            article.setCategoryId(category.getId());
        }

        // 设置用户ID
        article.setUserId(UserUtil.getUserDetailsDTO().getUserInfoId());

        // 保存或更新文章
        this.saveOrUpdate(article);

        // 保存文章标签信息
        saveArticleTag(articleVO, article.getId());

        // 如果文章状态为发布状态，将文章ID发送到消息队列
        if (article.getStatus().equals(1)) {
            rabbitTemplate.convertAndSend(RabbitMQConstant.SUBSCRIBE_EXCHANGE, "*", new Message(JSON.toJSONBytes(article.getId()), new MessageProperties()));
        }
    }


    /**
     * 修改文章是否置顶和推荐
     * 该方法根据提供的文章ID更新文章的置顶和精选状态。
     *
     * @param articleTopFeaturedVO 包含文章ID、置顶状态和精选状态的VO对象
     */
    @Override
    public void updateArticleTopAndFeatured(ArticleTopFeaturedVO articleTopFeaturedVO) {
        // 构建包含更新信息的 Article 对象
        Article article = Article.builder()
                .id(articleTopFeaturedVO.getId())
                .isTop(articleTopFeaturedVO.getIsTop())
                .isFeatured(articleTopFeaturedVO.getIsFeatured())
                .build();

        // 更新文章的置顶和精选状态
        articleMapper.updateById(article);
    }


    /**
     * 删除或者恢复文章
     * 该方法根据提供的文章ID列表批量更新文章的删除状态。
     *
     * @param deleteVO 包含文章ID列表和删除状态的 VO 对象
     */
    @Override
    public void updateArticleDelete(DeleteVO deleteVO) {
        // 使用流式处理将文章ID列表转换为包含更新信息的 Article 对象列表
        List<Article> articles = deleteVO.getIds().stream()
                .map(id -> Article.builder()
                        .id(id)
                        .isDelete(deleteVO.getIsDelete())
                        .build())
                .collect(Collectors.toList());

        // 批量删除或者恢复文章
        this.updateBatchById(articles);
    }


    /**
     * 物理删除文章
     * 该方法根据提供的文章ID列表批量删除文章及其相关的标签信息。
     *
     * @param articleIds 文章ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticles(List<Integer> articleIds) {
        // 删除与文章相关的标签信息
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>()
                .in(ArticleTag::getArticleId, articleIds));

        // 批量删除文章
        articleMapper.deleteBatchIds(articleIds);
    }


    /**
     * 获取后台文章详情
     * 该方法根据文章ID获取文章详情，包括分类名称和标签名称，并返回包含这些信息的 ArticleAdminViewDTO 对象。
     *
     * @param articleId 文章ID
     * @return 包含文章详情、分类名称和标签名称的 ArticleAdminViewDTO 对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleAdminViewDTO getArticleByIdAdmin(Integer articleId) {
        // 根据文章ID查询文章
        Article article = articleMapper.selectById(articleId);

        // 查询文章分类
        Category category = categoryMapper.selectById(article.getCategoryId());
        String categoryName = null;
        if (Objects.nonNull(category)) {
            categoryName = category.getCategoryName();
        }

        // 查询文章标签名称列表
        List<String> tagNames = tagMapper.listTagNamesByArticleId(articleId);

        // 将文章信息复制到 ArticleAdminViewDTO 对象
        ArticleAdminViewDTO articleAdminViewDTO = BeanCopyUtil.copyObject(article, ArticleAdminViewDTO.class);

        // 设置分类名称
        articleAdminViewDTO.setCategoryName(categoryName);

        // 设置标签名称列表
        articleAdminViewDTO.setTagNames(tagNames);

        // 返回包含文章详情、分类名称和标签名称的 ArticleAdminViewDTO 对象
        return articleAdminViewDTO;
    }


    /**
     * 导出文章
     * 该方法根据提供的文章ID列表导出文章内容，并上传生成的Markdown文件到指定位置，返回文件的URL列表。
     *
     * @param articleIds 文章ID列表
     * @return 包含导出文件URL的列表
     * @throws BizException 当导出文章失败时抛出异常
     */
    @Override
    public List<String> exportArticles(List<Integer> articleIds) {
        // 根据文章ID列表查询文章标题和内容
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .select(Article::getArticleTitle, Article::getArticleContent)
                .in(Article::getId, articleIds));

        // 存储文件URL的列表
        List<String> urls = new ArrayList<>();
        for (Article article : articles) {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(article.getArticleContent().getBytes())) {
                // 上传文章内容并获取文件URL
                String url = uploadStrategyContext.executeUploadStrategy(article.getArticleTitle() + FileExtEnum.MD.getExtName(), inputStream, FilePathEnum.MD.getPath());
                urls.add(url);
            } catch (Exception e) {
                e.printStackTrace();
                throw new BizException("导出文章失败");
            }
        }
        return urls;
    }


    /**
     * 搜索文章
     * 该方法根据提供的搜索条件查询文章，并返回包含搜索结果的列表。
     *
     * @param condition 包含搜索关键词的 VO 对象
     * @return 包含搜索结果的 ArticleSearchDTO 对象列表
     */
    @Override
    public List<ArticleSearchDTO> listArticlesBySearch(ConditionVO condition) {
        return searchStrategyContext.executeSearchStrategy(condition.getKeywords());
    }


    /**
     * 更新文章浏览次数
     * 该方法根据文章ID在Redis中增加文章的浏览次数。
     *
     * @param articleId 文章ID
     */
    public void updateArticleViewsCount(Integer articleId) {
        redisService.zIncr(RedisConstant.ARTICLE_VIEWS_COUNT, articleId, 1D);
    }


    /**
     * 保存文章分类
     * 该方法根据提供的文章信息保存文章分类，如果分类不存在且文章状态不是草稿，创建新的分类。
     *
     * @param articleVO 包含文章信息的 VO 对象
     * @return 保存或新建的 Category 对象
     */
    private Category saveArticleCategory(ArticleVO articleVO) {
        // 查询分类是否存在
        Category category = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getCategoryName, articleVO.getCategoryName()));

        // 如果分类不存在且文章状态不是草稿，创建新的分类
        if (Objects.isNull(category) && !articleVO.getStatus().equals(ArticleStatusEnum.DRAFT.getStatus())) {
            category = Category.builder()
                    .categoryName(articleVO.getCategoryName())
                    .build();
            categoryMapper.insert(category);
        }
        return category;
    }


    /**
     * 保存文章标签
     * 该方法根据提供的文章信息和文章ID保存文章标签。
     * 首先删除已有的文章标签，然后保存新标签。
     *
     * @param articleVO 包含文章信息的 VO 对象
     * @param articleId 文章ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveArticleTag(ArticleVO articleVO, Integer articleId) {
        // 如果文章ID不为空，删除已有的文章标签
        if (Objects.nonNull(articleVO.getId())) {
            articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>()
                    .eq(ArticleTag::getArticleId, articleVO.getId()));
        }

        // 获取文章的标签名称列表
        List<String> tagNames = articleVO.getTagNames();
        if (CollectionUtils.isNotEmpty(tagNames)) {
            // 查询已存在的标签
            List<Tag> existTags = tagService.list(new LambdaQueryWrapper<Tag>()
                    .in(Tag::getTagName, tagNames));
            List<String> existTagNames = existTags.stream()
                    .map(Tag::getTagName)
                    .collect(Collectors.toList());
            List<Integer> existTagIds = existTags.stream()
                    .map(Tag::getId)
                    .collect(Collectors.toList());

            // 移除已存在的标签名称
            tagNames.removeAll(existTagNames);
            if (CollectionUtils.isNotEmpty(tagNames)) {
                // 保存新标签
                List<Tag> tags = tagNames.stream().map(item -> Tag.builder()
                                .tagName(item)
                                .build())
                        .collect(Collectors.toList());
                tagService.saveBatch(tags);
                List<Integer> tagIds = tags.stream()
                        .map(Tag::getId)
                        .collect(Collectors.toList());
                existTagIds.addAll(tagIds);
            }

            // 将所有标签与文章关联
            List<ArticleTag> articleTags = existTagIds.stream().map(item -> ArticleTag.builder()
                            .articleId(articleId)
                            .tagId(item)
                            .build())
                    .collect(Collectors.toList());
            articleTagService.saveBatch(articleTags);
        }
    }


}
