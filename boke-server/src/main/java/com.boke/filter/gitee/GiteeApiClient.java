package com.boke.filter.gitee;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;


@Component
public class GiteeApiClient {
    @Autowired
    ObjectMapper objectMapper;

    private RestTemplate restTemplate = new RestTemplate();

    @Value("${login.gitee.clientId}")
    private String clientId;

    @Value("${login.gitee.clientSecret}")
    private String clientSecret;

    @Value("${login.gitee.redirectUri}")
    private String redirectUri;

    /**
     * 前提：前端发送请求获取授权码
     * https://gitee.com/oauth/authorize?client_id=a40d6cc1324eb76b38cfcec264dcee5e754f22e9c4e7113b6f44782fead10668&redirect_uri=http://localhost:8080/user/login/gitee&response_type=code
     *
     * 通过授权码获取GiteeToken
     *
     * @param code 授权码
     * @return
     */
    public String getTokenByCode(String code) throws JsonProcessingException {
        String url = "https://gitee.com/oauth/token?grant_type=authorization_code"
                + "&code=" + code
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&client_secret=" + clientSecret;
        String responseJson = sendPostRequest(url, null);
        Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
        Object accessToken = responseMap.get("access_token");
        if (accessToken == null) {
            throw new RuntimeException("{gitee.get.token.fail:获取GiteeToken失败！}");
        }
        return accessToken.toString();
    }

    /**
     * 通过GiteeToken获取用户信息
     *
     * @param token
     * @return
     * @throws JsonProcessingException
     */
    public Map<String, Object> getThirdUserInfo(String token) throws JsonProcessingException {
        // {"id":1483966,"login":"用户名"}
        String responseJson = sendGetRequest("https://gitee.com/api/v5/user?access_token=" + token);
        Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
        Object openId = responseMap.get("id");
        if (openId == null) {
            return null;
        }

        HashMap<String, Object> thirdUser = new HashMap<>();
        thirdUser.put("openId", openId);
        thirdUser.put("nickname", responseMap.get("login"));
        return thirdUser;
    }

    public String sendPostRequest(String url, Map<String, Object> body) {
        // 请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 创建HttpEntity对象
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        // 发送POST请求
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
                    String.class);
            // 返回响应体
            return response.getBody();
        } catch (HttpClientErrorException e) {
            return e.getResponseBodyAsString();
        }
    }

    public String sendGetRequest(String url) {
        // 请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 创建HttpEntity对象
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(headers);
        // 发送GET请求
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity,
                    String.class);
            // 返回响应体
            return response.getBody();
        } catch (HttpClientErrorException e) {
            return e.getResponseBodyAsString();
        }
    }
}

