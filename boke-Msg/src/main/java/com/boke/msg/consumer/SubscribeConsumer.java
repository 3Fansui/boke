package com.boke.msg.consumer;


import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.boke.db.entity.Article;
import com.boke.db.entity.UserInfo;
import com.boke.model.dto.ArticleDTO;
import com.boke.model.dto.EmailDTO;
import com.boke.db.service.ArticleService;
import com.boke.db.service.UserInfoService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boke.model.vo.ResultVO;
import com.boke.msg.util.EmailUtil;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.boke.constant.CommonConstant.TRUE;
import static com.boke.constant.RabbitMQConstant.SUBSCRIBE_QUEUE;

@Component
@RabbitListener(queues = SUBSCRIBE_QUEUE)
public class SubscribeConsumer {

    @Value("${website.url}")
    private String websiteUrl;
    @Autowired
    private RestTemplate restTemplate;


    @Autowired
    private EmailUtil emailUtil;

    @RabbitHandler
    public void process(byte[] data) {
        Integer articleId = JSON.parseObject(new String(data), Integer.class);
        Article article = BeanUtil.copyProperties(getArticle(articleId), Article.class);
        List<UserInfo> users = getUserInfo();
        List<String> emails = users.stream().map(UserInfo::getEmail).collect(Collectors.toList());
        for (String email : emails) {
            EmailDTO emailDTO = new EmailDTO();
            Map<String, Object> map = new HashMap<>();
            emailDTO.setEmail(email);
            emailDTO.setSubject("文章订阅");
            emailDTO.setTemplate("common.html");
            String url = websiteUrl + "/articles/" + articleId;
            if (article.getUpdateTime() == null) {
                map.put("content", "花未眠的个人博客发布了新的文章，"
                        + "<a style=\"text-decoration:none;color:#12addb\" href=\"" + url + "\">点击查看</a>");
            } else {
                map.put("content", "花未眠的个人博客对《" + article.getArticleTitle() + "》进行了更新，"
                        + "<a style=\"text-decoration:none;color:#12addb\" href=\"" + url + "\">点击查看</a>");
            }
            emailDTO.setCommentMap(map);
            emailUtil.sendHtmlMail(emailDTO);
        }
    }
    public ArticleDTO getArticle(Integer articleId) {
        String url = "http://localhost:8080/articles/{articleId}";
        // 仅获取返回体里的 JSON 并尝试反序列化成 ArticleDTO
        // 适用于后端直接返回 ArticleDTO 时
        return restTemplate.getForObject(url, ArticleDTO.class, articleId);
    }
    public List<UserInfo> getUserInfo() {
        String url = "http://localhost:8080/users/subscribe";

        // 构造请求实体（GET 请求通常不需要请求体，可以传 null）
        HttpEntity<Void> requestEntity = null;

        // 利用 exchange 并指定返回类型是 ResultVO<List<UserInfo>>
        ResponseEntity<ResultVO<List<UserInfo>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<ResultVO<List<UserInfo>>>() {}
        );

        // 从响应体中获取真正的返回对象
        return Objects.requireNonNull(response.getBody()).getData();
    }

}
