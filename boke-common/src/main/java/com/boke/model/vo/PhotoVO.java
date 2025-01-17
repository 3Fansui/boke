package com.boke.model.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "照片")
public class PhotoVO {

    @NotNull(message = "相册id不能为空")
    @Schema(name = "id", description = "相册id", requiredMode = Schema.RequiredMode.REQUIRED, type = "Integer")
    private Integer albumId;

    @Schema(name = "photoUrlList", description = "照片列表", requiredMode = Schema.RequiredMode.REQUIRED, type = "List<String>")
    private List<String> photoUrls;

    @Schema(name = "photoIdList", description = "照片id列表", requiredMode = Schema.RequiredMode.REQUIRED, type = "List<Integer>")
    private List<Integer> photoIds;

}
