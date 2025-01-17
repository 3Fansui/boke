package com.boke.strategy.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.analysis.Analyzer;
import co.elastic.clients.elasticsearch._types.analysis.CustomAnalyzer;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.boke.annotation.ElasticsearchField;
import com.boke.annotation.ElasticsearchIndex;
import com.boke.model.dto.ArticleSearchDTO;
import com.boke.strategy.SearchStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 为了实现策略模式，此工具类实现了SearchStrategy接口
 */
@Service("esSearchStrategyImpl")
@Slf4j
public class ElasticsearchUtil implements SearchStrategy {
    @Autowired
    ElasticsearchClient esClient;
    @Autowired
    ObjectMapper objectMapper;

    @PostConstruct
    public void init() throws ClassNotFoundException, IllegalAccessException {
        Field[] fields = Class.forName("com.boke.constant.EsConstant").getFields();
        for (Field field : fields) {
            if (field.getType().equals(String.class)) {
                createIndex((String) field.get(null));
            }
        }

    }



    @Override
    public List<ArticleSearchDTO> searchArticle(String keywords) {
        SearchResponse<Object> response = searchDocument(ArticleSearchDTO.class, keywords);
        List<ArticleSearchDTO> articleSearchDTOS = new ArrayList<>();
        response.hits().hits().forEach(hit -> {
            ArticleSearchDTO articleSearchDTO = objectMapper.convertValue(hit.source(), ArticleSearchDTO.class);
            articleSearchDTOS.add(articleSearchDTO);
        });
        return articleSearchDTOS;
    }

    /**
     * 创建索引
     *
     * @param indexName 索引名称
     */
    public void createIndex(String indexName) {
        try {
            if (!esClient.indices().exists(c -> c.index(indexName)).value()) {
                //定义两个自定义分析器 ik_max_word 和 ik_smart，并可选地添加 lowercase 过滤器
                Analyzer ikMaxWordAnalyzer = Analyzer.of(a -> a
                        .custom(CustomAnalyzer.of(ca -> ca
                                .tokenizer("ik_max_word")
                                .filter("lowercase") // 可选：添加其他过滤器
                        ))
                );
                Analyzer ikSmartAnalyzer = Analyzer.of(a -> a
                        .custom(CustomAnalyzer.of(ca -> ca
                                .tokenizer("ik_smart")
                                .filter("lowercase")
                        ))
                );
                IndexSettings settings = IndexSettings.of(s -> s
                        .numberOfShards("1") //设置索引的分片数、副本数以及分析器配置。
                        .numberOfReplicas("1")
                        .analysis(a -> a
                                .analyzer("ik_max_word", ikMaxWordAnalyzer)
                                .analyzer("ik_smart", ikSmartAnalyzer)
                        )
                );
                //指定 articleTitle 字段使用 ik_max_word 分词器，articleContent 字段使用 ik_smart 分词器。
                Map<String, Property> mapping = new HashMap<>();
                mapping.put("articleTitle", Property.of(p -> p
                        .text(TextProperty.of(t -> t
                                .analyzer("ik_max_word") // 使用 ik_max_word 分词器
                        ))
                ));
                mapping.put("articleContent", Property.of(p -> p
                        .text(TextProperty.of(t -> t
                                .analyzer("ik_smart") // 使用 ik_smart 分词器
                        ))
                ));

                esClient.indices().create(c -> c.index(indexName).settings(settings).mappings(m -> m.properties(mapping)));
            }
        } catch (Exception e) {
            log.error("创建索引失败{}", e.getMessage());
        }
    }

    /**
     * 删除索引
     *
     * @param indexName 索引名称
     */
    public void deleteIndex(String indexName) {
        try {
            esClient.indices().delete(c -> c.index(indexName));
        } catch (Exception e) {
            log.error("删除索引失败{}", e.getMessage());
        }
    }

    /**
     * 添加文档
     *
     * @param indexName 索引名称
     * @param id        文档id
     */
    public void addDocument(String indexName, String id, Object obj) {
        try {
            IndexResponse response = esClient.index(c -> c.index(indexName)
                    .id(id)
                    .document(obj)
            );

        } catch (Exception e) {
            log.error("添加文档失败", e.getCause());
        }
    }

    /**
     * 批量添加文档
     * @param indexName
     * @param objs
     */
    public void addBatchDocuments(String indexName, List<ArticleSearchDTO> objs) {
        try {
            BulkRequest.Builder br = new BulkRequest.Builder();

            for (ArticleSearchDTO article : objs) {
                br.operations(op -> op
                        .index(c-> c
                                .index(indexName)
                                .id(String.valueOf(article.getId()))
                                .document(article)
                        )
                );
            }
            BulkResponse result = esClient.bulk(br.build());
            if (result.errors()) {
                for (BulkResponseItem item : result.items()) {
                    if (item.error() != null) {
                        log.error("添加文档失败,具体原因：{}", item.error().reason());
                    }
                }
            }
        } catch (Exception e) {
            log.error("批量添加文档失败", e.getCause());
        }
    }

    /**
     * 获取文档
     *
     * @param indexName 索引名称
     * @param id        文档id
     * @param clazz     返回对象类型
     */
    public <T> T getDocument(String indexName, String id, Class<T> clazz) {
        try {
            GetResponse response = esClient.get(g -> g
                            .index(indexName)  // 动态使用传入的索引名称
                            .id(id),           // 传入要查询的文档ID
                    clazz               // 反序列化返回结果为clazz类型
            );

            if (response.found()) {
                // 将源数据反序列化为T类
                Object source = response.source();
                T t = objectMapper.convertValue(source, clazz);
                // 处理返回的document
                return t;
            } else {
                log.error("Document not found.");
            }
        } catch (Exception e) {
            log.error("获取文档失败{}", e.getMessage());
        }
        return null;
    }

    /**
     * 更新文档
     *
     * @param indexName 索引名称
     * @param id        文档id
     * @param obj       对象
     */
    public void updateDocument(String indexName, String id, Object obj) {
        try {
            esClient.update(u -> u
                            .index(indexName)
                            .id(id)
                            .upsert(obj)
                    , Object.class
            );
        } catch (Exception e) {
            log.error("更新文档失败", e.getCause());
        }
    }

    /**
     * 删除文档
     *
     * @param indexName
     * @param id
     */
    public void deleteDocument(String indexName, String id) {
        try {
            esClient.delete(d -> d.index(indexName).id(id));
        } catch (Exception e) {
            log.error("删除文档失败{}", e.getMessage());
        }
    }

    //批量删除文档
    public void deleteBatchDocument(String indexName, List<String> ids) {
        try {
            DeleteByQueryResponse response = esClient.deleteByQuery(d -> d
                    .index(indexName)
                    .query(q -> q
                            .terms(t -> t
                                    .field("id") // 如果使用 Elasticsearch 的 _id 字段，改为 "_id"
                                    .terms(tr -> tr.value(ids.stream().map(FieldValue::of).collect(Collectors.toList())))
                            )
                    )
            );
            log.info("成功删除 {} 个文档", response.deleted());
        } catch (Exception e) {
            log.error("批量删除文档失败{}", e.getMessage());
        }
    }
    //刪除索引中的所有文档
    public void deleteAllDocuments(String indexName) {
        try {
            DeleteByQueryResponse response = esClient.deleteByQuery(d -> d
                    .index(indexName)
                    .query(q -> q
                            .matchAll(m -> m)
                    )
            );
        } catch (Exception e) {
            log.error("删除索引中的所有文档失败{}", e.getMessage());
        }
    }

    /**
     * 精准搜索
     *
     * @param indexName  索引名称，可以是多个
     * @param field      字段名称
     * @param searchText 关键词
     * @return 搜索结果
     */
    public SearchResponse<Object> searchDocument(String field, String searchText, String... indexName) {
        try {
            SearchResponse<Object> response = esClient.search(s -> s
                            .index(Arrays.asList(indexName))
                            .query(q -> q
                                    .match(t -> t
                                            .field(field)
                                            .query(searchText)
                                    )
                            ),
                    Object.class
            );
            return response;
        } catch (IOException e) {
            log.info("搜索文档失败", e.getCause());
        }
        return null;
    }

    /**
     * 搜索文档
     *
     * @param dtoClass   DTO 类
     * @param searchText 搜索文本
     * @return 搜索响应
     */
    public <T> SearchResponse<Object> searchDocument(Class<T> dtoClass, String searchText) {
        try {
            // 获取索引名称
            ElasticsearchIndex indexAnnotation = dtoClass.getAnnotation(ElasticsearchIndex.class);
            if (indexAnnotation == null) {
                throw new IllegalArgumentException("DTO类没有ElasticsearchIndex注解");
            }
            String indexName = indexAnnotation.indexName();

            // 获取所有带有ElasticsearchField注解的字段
            List<String> fields = new ArrayList<>();
            for (Field field : dtoClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(ElasticsearchField.class)) {
                    ElasticsearchField fieldAnnotation = field.getAnnotation(ElasticsearchField.class);
                    fields.add(fieldAnnotation.fieldName());
                }
            }


            // 构建多字段查询
            SearchResponse<Object> response = esClient.search(s -> s
                            .index(indexName)
                            .query(q -> q.multiMatch(m -> m
                                    .query(searchText)
                                    .fields(fields)
                            ))

                    , Object.class
            );
            return response;
        } catch (Exception e) {
            log.error("搜索文档失败: {}", e.getMessage());
        }
        return null;
    }


}
