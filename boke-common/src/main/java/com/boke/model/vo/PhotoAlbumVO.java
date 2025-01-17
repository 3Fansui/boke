package com.boke.model.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "相册")
public class PhotoAlbumVO {

    @Schema(name = "id", description = "相册id",requiredMode = Schema.RequiredMode.REQUIRED, type = "Integer")
    private Integer id;

    @NotBlank(message = "相册名不能为空")
    @Schema(name = "albumName", description = "相册名",requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String albumName;

    @Schema(name = "albumDesc", description = "相册描述", type = "String")
    private String albumDesc;

    @NotBlank(message = "相册封面不能为空")
    @Schema(name = "albumCover", description = "相册封面",requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String albumCover;

    @Schema(name = "status", description = "状态值",requiredMode = Schema.RequiredMode.REQUIRED, type = "Integer")
    private Integer status;

}
