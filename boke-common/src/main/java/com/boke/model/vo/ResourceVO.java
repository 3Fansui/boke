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
@Schema(description = "资源")
public class ResourceVO {

    @Schema(name = "id", description = "资源id", requiredMode = Schema.RequiredMode.REQUIRED, type = "Integer")
    private Integer id;

    @NotBlank(message = "资源名不能为空")
    @Schema(name = "resourceName", description = "资源名", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String resourceName;

    @Schema(name = "url", description = "资源路径", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String url;

    @Schema(name = "url", description = "资源路径", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String requestMethod;

    @Schema(name = "parentId", description = "父资源id", requiredMode = Schema.RequiredMode.REQUIRED, type = "Integer")
    private Integer parentId;

    @Schema(name = "isAnonymous", description = "是否匿名访问", requiredMode = Schema.RequiredMode.REQUIRED, type = "Integer")
    private Integer isAnonymous;

}
