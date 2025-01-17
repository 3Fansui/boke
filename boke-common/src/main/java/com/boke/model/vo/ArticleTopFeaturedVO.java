package com.boke.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleTopFeaturedVO {
    @NotNull(message = "id不能为空")
    private Integer id;

    @NotNull(message = "是否置顶不能为空")
    private Integer isTop;

    @NotNull(message = "是否推荐不能为空")
    private Integer isFeatured;
}
