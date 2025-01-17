package com.boke.model.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "用户权限")
public class UserRoleVO {

    @NotNull(message = "id不能为空")
    @Schema(name = "userInfoId", description = "用户信息id", type = "Integer")
    private Integer userInfoId;

    @NotBlank(message = "昵称不能为空")
    @Schema(name = "nickname", description = "昵称", type = "String")
    private String nickname;

    @NotNull(message = "用户角色不能为空")
    @Schema(name = "roleList", description = "角色id集合", type = "List<Integer>")
    private List<Integer> roleIds;

}
