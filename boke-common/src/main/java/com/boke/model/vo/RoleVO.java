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
@Schema(description = "角色")
public class RoleVO {

    @Schema(name = "id", description = "用户id", type = "Integer")
    private Integer id;

    @NotBlank(message = "角色名不能为空")
    @Schema(name = "roleName", description = "角色名", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String roleName;

    @Schema(name = "resourceIdList", description = "资源列表", requiredMode = Schema.RequiredMode.REQUIRED, type = "List<Integer>")
    private List<Integer> resourceIds;

    @Schema(name = "menuIdList", description = "菜单列表", requiredMode = Schema.RequiredMode.REQUIRED, type = "List<Integer>")
    private List<Integer> menuIds;

}
