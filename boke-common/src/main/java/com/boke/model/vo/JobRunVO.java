package com.boke.model.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRunVO {

    @Schema(name="任务id" ,description = "id",requiredMode = Schema.RequiredMode.REQUIRED, type = "Integer")
    private Integer id;

    @Schema(name="任务组别" ,description = "jobGroup",requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String jobGroup;
}
