package com.boke.model.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "用户禁用状态")
public class UserDisableVO {

    @NotNull(message = "用户id不能为空")
    private Integer id;

    @NotNull(message = "用户禁用状态不能为空")
    private Integer isDisable;

}
