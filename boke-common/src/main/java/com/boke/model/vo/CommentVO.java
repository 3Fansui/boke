package com.boke.model.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "评论")
public class CommentVO {

    @Schema(name = "replyUserId", description = "回复用户id", type = "Integer")
    private Integer replyUserId;

    @Schema(name = "topicId", description = "主题id", type = "Integer")
    private Integer topicId;

    @NotBlank(message = "评论内容不能为空")
    @Schema(name = "commentContent", description = "评论内容", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String commentContent;

    @Schema(name = "parentId", description = "评论父id", type = "Integer")
    private Integer parentId;

    @NotNull(message = "评论类型不能为空")
    @Schema(name = "type", description = "评论类型", type = "Integer")
    private Integer type;
}
