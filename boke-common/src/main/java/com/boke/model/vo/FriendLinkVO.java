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
@Schema(description = "友链")
public class FriendLinkVO {

    @Schema(name = "categoryId", description = "友链id", type = "Integer")
    private Integer id;

    @NotBlank(message = "链接名不能为空")
    @Schema(name = "linkName", description = "友链名", type = "String", requiredMode = Schema.RequiredMode.REQUIRED)
    private String linkName;

    @NotBlank(message = "链接头像不能为空")
    @Schema(name = "linkAvatar", description = "友链头像", type = "String", requiredMode = Schema.RequiredMode.REQUIRED)
    private String linkAvatar;

    @NotBlank(message = "链接地址不能为空")
    @Schema(name = "linkAddress", description = "友链头像", type = "String", requiredMode = Schema.RequiredMode.REQUIRED)
    private String linkAddress;

    @NotBlank(message = "链接介绍不能为空")
    @Schema(name = "linkIntro", description = "友链头像", type = "String", requiredMode = Schema.RequiredMode.REQUIRED)
    private String linkIntro;

}
