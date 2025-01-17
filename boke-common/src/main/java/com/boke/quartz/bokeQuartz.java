package com.boke.quartz;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson.JSON;
import com.boke.model.dto.ArticleSearchDTO;
import com.boke.model.dto.UserAreaDTO;
import com.boke.entity.*;

import com.boke.mapper.UniqueViewMapper;
import com.boke.mapper.UserAuthMapper;
import com.boke.service.*;
import com.boke.strategy.impl.ElasticsearchUtil;
import com.boke.util.BeanCopyUtil;
import com.boke.util.IpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.boke.constant.CommonConstant.UNKNOWN;
import static com.boke.constant.EsConstant.ARTICLE_INDEX;
import static com.boke.constant.RedisConstant.*;

@Slf4j
@Component("auroraQuartz")
public class bokeQuartz {

    @Autowired
    private RedisService redisService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private JobLogService jobLogService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private RoleResourceService roleResourceService;

    @Autowired
    private UniqueViewMapper uniqueViewMapper;

    @Autowired
    private UserAuthMapper userAuthMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ElasticsearchUtil elasticsearchUtil;


    @Value("${website.url}")
    private String websiteUrl;

    /**
     * 保存访客的统计数据到数据库
     */
    public void saveUniqueView() {
        Long count = redisService.sSize(UNIQUE_VISITOR);
        UniqueView uniqueView = UniqueView.builder()
                .createTime(LocalDateTimeUtil.offset(LocalDateTime.now(), -1, ChronoUnit.DAYS))
                .viewsCount(Optional.of(count.intValue()).orElse(0))
                .build();
        uniqueViewMapper.insert(uniqueView);
    }

    public void clear() {
        redisService.del(UNIQUE_VISITOR);
        redisService.del(VISITOR_AREA);
    }

    /**
     * 统计用户所在地区，并将结果存储到Redis中
     */
    public void statisticalUserArea() {
        Map<String, Long> userAreaMap = userAuthMapper.selectList(new LambdaQueryWrapper<UserAuth>().select(UserAuth::getIpSource))
                .stream()
                .map(item -> {
                    if (Objects.nonNull(item) && StringUtils.isNotBlank(item.getIpSource())) {
                        return IpUtil.getIpProvince(item.getIpSource());
                    }
                    return UNKNOWN;
                })
                .collect(Collectors.groupingBy(item -> item, Collectors.counting()));
        List<UserAreaDTO> userAreaList = userAreaMap.entrySet().stream()
                .map(item -> UserAreaDTO.builder()
                        .name(item.getKey())
                        .value(item.getValue())
                        .build())
                .collect(Collectors.toList());
        redisService.set(USER_AREA, JSON.toJSONString(userAreaList));
    }

    /**
     * 将博客系统中的所有文章ID获取出来，并依次向百度提交这些文章的URL，以便进行SEO优化。
     */
    public void baiduSeo() {
        List<Integer> ids = articleService.list().stream().map(Article::getId).collect(Collectors.toList());
        HttpHeaders headers = new HttpHeaders();
        headers.add("Host", "data.zz.baidu.com");
        headers.add("User-Agent", "curl/7.12.1");
        headers.add("Content-Length", "83");
        headers.add("Content-Type", "text/plain");
        ids.forEach(item -> {
            String url = websiteUrl + "/articles/" + item;
            HttpEntity<String> entity = new HttpEntity<>(url, headers);
            restTemplate.postForObject("https://www.baidu.com", entity, String.class);
        });
    }

    public void clearJobLogs() {
        jobLogService.cleanJobLogs();
    }

    /**
     * 从swagger文档中获取接口信息，并保存到数据库中
     */
    public void importSwagger() {
        resourceService.importSwagger();
        List<Integer> resourceIds = resourceService.list().stream().map(Resource::getId).collect(Collectors.toList());
        List<RoleResource> roleResources = new ArrayList<>();
        for (Integer resourceId : resourceIds) {
            roleResources.add(RoleResource.builder()
                    .roleId(1)
                    .resourceId(resourceId)
                    .build());
        }
        roleResourceService.saveBatch(roleResources);
    }

    public void importDataIntoES()  {
        elasticsearchUtil.deleteAllDocuments(ARTICLE_INDEX);
        List<Article> articles = articleService.list();
        List<ArticleSearchDTO> articleSearchDTOS = articles.stream().map(item -> BeanCopyUtil.copyObject(item, ArticleSearchDTO.class)).toList();
        elasticsearchUtil.addBatchDocuments(ARTICLE_INDEX, articleSearchDTOS);
    }
}
