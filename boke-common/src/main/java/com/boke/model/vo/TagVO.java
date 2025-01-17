package com.boke.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "标签对象")
public class TagVO {

    @Schema(name = "id", description = "标签id", type = "Integer")
    private Integer id;

    @NotBlank(message = "标签名不能为空")
    @Schema(name = "categoryName", description = "标签名", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String tagName;

}
