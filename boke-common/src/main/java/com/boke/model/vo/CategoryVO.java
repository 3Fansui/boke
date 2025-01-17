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
@Schema(description = "分类")
public class CategoryVO {

    @Schema(name = "id", description = "分类id", type = "Integer")
    private Integer id;

    @NotBlank(message = "分类名不能为空")
    @Schema(name = "categoryName", description = "分类名", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String categoryName;

}
