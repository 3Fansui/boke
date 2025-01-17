package com.boke.model.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "查询条件")
public class ConditionVO {

    @Schema(name = "current", description = "页码", type = "Long")
    private Long current;

    @Schema(name = "size", description = "条数", type = "Long")
    private Long size;

    @Schema(name = "keywords", description = "搜索内容", type = "String")
    private String keywords;

    @Schema(name = "categoryId", description = "分类id", type = "Integer")
    private Integer categoryId;

    @Schema(name = "tagId", description = "标签id", type = "Integer")
    private Integer tagId;

    @Schema(name = "albumId", description = "相册id", type = "Integer")
    private Integer albumId;

    @Schema(name = "loginType", description = "登录类型", type = "Integer")
    private Integer loginType;

    @Schema(name = "type", description = "类型", type = "Integer")
    private Integer type;

    @Schema(name = "status", description = "状态", type = "Integer")
    private Integer status;

    @Schema(name = "startTime", description = "开始时间", type = "LocalDateTime")
    private LocalDateTime startTime;

    @Schema(name = "endTime", description = "结束时间", type = "LocalDateTime")
    private LocalDateTime endTime;

    @Schema(name = "isDelete", description = "是否删除", type = "Integer")
    private Integer isDelete;

    @Schema(name = "isReview", description = "是否审核", type = "Integer")
    private Integer isReview;

    @Schema(name = "isTop", description = "是否置顶", type = "Integer")
    private Integer isTop;

    @Schema(name = "isFeatured", description = "是否推荐", type = "Integer")
    private Integer isFeatured;


}
