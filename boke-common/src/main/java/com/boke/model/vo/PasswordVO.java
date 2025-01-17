package com.boke.model.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "密码")
public class PasswordVO {

    @NotBlank(message = "旧密码不能为空")
    @Schema(name = "oldPassword", description = "旧密码", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String oldPassword;

    @Size(min = 6, message = "新密码不能少于6位")
    @NotBlank(message = "新密码不能为空")
    @Schema(name = "newPassword", description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String newPassword;
}
