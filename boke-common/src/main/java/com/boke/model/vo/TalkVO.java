package com.boke.model.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "说说对象")
public class TalkVO {

    @Schema(name = "id", description = "说说id", type = "Integer")
    private Integer id;

    @Schema(name = "content", description = "说说内容", type = "String")
    @NotBlank(message = "说说内容不能为空")
    private String content;

    @Schema(name = "images", description = "说说图片", type = "String")
    private String images;

    @Schema(name = "isTop", description = "置顶状态", type = "Integer")
    @NotNull(message = "置顶状态不能为空")
    private Integer isTop;

    @Schema(name = "status", description = "说说状态", type = "Integer")
    @NotNull(message = "说说状态不能为空")
    private Integer status;

}
