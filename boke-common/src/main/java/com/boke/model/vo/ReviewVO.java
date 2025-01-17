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
@Schema(description = "审核")
public class ReviewVO {

    @NotNull(message = "id不能为空")
    @Schema(name = "idList", description = "id列表", requiredMode = Schema.RequiredMode.REQUIRED, type = "List<Integer>")
    private List<Integer> ids;

    @NotNull(message = "状态值不能为空")
    @Schema(name = "isDelete", description = "删除状态", requiredMode = Schema.RequiredMode.REQUIRED, type = "Integer")
    private Integer isReview;

}
