package com.boke.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "绑定邮箱")
public class EmailVO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(name = "email", description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Schema(name = "code", description = "邮箱验证码", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String code;

}
