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
public class JobLogSearchVO {

    @Schema(name = "任务Id", description = "jobId", type = "Integer")
    private Integer jobId;

    @Schema(name = "任务名称", description = "jobName", type = "String")
    private String jobName;

    @Schema(name = "任务的组别", description = "jobGroup", type = "String")
    private String jobGroup;

    @Schema(name = "任务状态", description = "status", type = "Integer")
    private Integer status;

    @Schema(name = "开始时间", description = "startTime", type = "String")
    private String startTime;

    @Schema(name = "结束时间", description = "endTime", type = "String")
    private String endTime;
}
