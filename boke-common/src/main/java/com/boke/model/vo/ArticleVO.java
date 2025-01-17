package com.boke.model.vo;



import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "文章")
public class ArticleVO {

    @Schema(name = "id", description = "文章id", type = "Integer")
    private Integer id;

    @NotBlank(message = "文章标题不能为空")
    @Schema(name = "articleTitle", description = "文章标题", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String articleTitle;

    @NotBlank(message = "文章内容不能为空")
    @Schema(name = "articleContent", description = "文章内容", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String articleContent;

    @Schema(name = "articleAbstract", description = "文章摘要", type = "String")
    private String articleAbstract;

    @Schema(name = "articleCover", description = "文章缩略图", type = "String")
    private String articleCover;

    @Schema(name = "category", description = "文章分类", type = "Integer")
    private String categoryName;

    @Schema(name = "tagNameList", description = "文章标签", type = "List<Integer>")
    private List<String> tagNames;

    @Schema(name = "isTop", description = "是否置顶", type = "Integer")
    private Integer isTop;

    @Schema(name = "isFeatured", description = "是否推荐", type = "Integer")
    private Integer isFeatured;

    @Schema(name = "status", description = "文章状态", type = "String")
    private Integer status;

    @Schema(name = "type", description = "文章类型", type = "Integer")
    private Integer type;

    @Schema(name = "originalUrl", description = "原文链接", type = "String")
    private String originalUrl;

    @Schema(name = "password", description = "文章访问密码", type = "String")
    private String password;
}
