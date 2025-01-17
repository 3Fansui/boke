package com.boke.model.dto;

import com.boke.annotation.ElasticsearchField;
import com.boke.annotation.ElasticsearchIndex;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

@ElasticsearchIndex(indexName = "article")
public class ArticleSearchDTO {


    private Integer id;


    @ElasticsearchField(fieldName = "articleTitle")
    private String articleTitle;


    @ElasticsearchField(fieldName = "articleContent")
    private String articleContent;


    private Integer isDelete;


    private Integer status;

}
