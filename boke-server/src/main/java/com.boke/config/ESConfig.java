package com.boke.config;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;

import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@Slf4j
@ConfigurationProperties(prefix = "es")
@Data
public class ESConfig {

    private String serverUrl;
    private String apiKey;
    @Autowired
    // 创建带有 JavaTimeModule 的 ObjectMapper
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());


    @Bean
    public ElasticsearchClient esClient() throws Exception {

        // Create the low-level client
        RestClient restClient = RestClient
                .builder(HttpHost.create(serverUrl))
                .setDefaultHeaders(new Header[]{
                        new BasicHeader("Authorization", "ApiKey " + apiKey)
                })
                .build();
        // Create the transport with a Jackson mapper
        JacksonJsonpMapper jacksonJsonpMapper= new JacksonJsonpMapper(objectMapper);
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, jacksonJsonpMapper);
        // And create the API client
        ElasticsearchClient esClient = new ElasticsearchClient(transport);
        log.info("esClient init success");
        return esClient;
    }


}
