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
@Schema(description = "菜单")
public class MenuVO {

    @Schema(name = "id", description = "菜单id", type = "Integer")
    private Integer id;

    @NotBlank(message = "菜单名不能为空")
    @Schema(name = "name", description = "菜单名", type = "String")
    private String name;

    @NotBlank(message = "菜单icon不能为空")
    @Schema(name = "icon", description = "菜单icon", type = "String")
    private String icon;

    @NotBlank(message = "路径不能为空")
    @Schema(name = "path", description = "路径", type = "String")
    private String path;

    @NotBlank(message = "组件不能为空")
    @Schema(name = "component", description = "组件", type = "String")
    private String component;

    @NotNull(message = "排序不能为空")
    @Schema(name = "orderNum", description = "排序", type = "Integer")
    private Integer orderNum;

    @Schema(name = "parentId", description = "父id", type = "Integer")
    private Integer parentId;

    @Schema(name = "isHidden", description = "是否隐藏", type = "Integer")
    private Integer isHidden;

}
