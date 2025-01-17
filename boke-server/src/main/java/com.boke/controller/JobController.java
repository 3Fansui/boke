package com.boke.controller;

import com.boke.annotation.OptLog;
import com.boke.model.dto.JobDTO;
import com.boke.model.dto.PageResultDTO;
import com.boke.service.JobService;
import com.boke.model.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.boke.constant.OptTypeConstant.*;

@Tag(name = "定时任务模块")
@RestController
public class JobController {

    @Autowired
    private JobService jobService;

    @OptLog(optType = SAVE)
    @Operation(summary = "添加定时任务")
    @PostMapping("/admin/jobs")
    public ResultVO<?> saveJob(@RequestBody JobVO jobVO) {
        jobService.saveJob(jobVO);
        return ResultVO.ok();
    }

    @OptLog(optType = UPDATE)
    @Operation(summary = "修改定时任务")
    @PutMapping("/admin/jobs")
    public ResultVO<?> updateJob(@RequestBody JobVO jobVO) {
        jobService.updateJob(jobVO);
        return ResultVO.ok();
    }

    @OptLog(optType = DELETE)
    @Operation(summary = "删除定时任务")
    @DeleteMapping("/admin/jobs")
    public ResultVO<?> deleteJobById(@RequestBody List<Integer> jobIds) {
        jobService.deleteJobs(jobIds);
        return ResultVO.ok();
    }

    @Operation(summary = "根据id获取任务")
    @GetMapping("/admin/jobs/{id}")
    public ResultVO<JobDTO> getJobById(@PathVariable("id") Integer jobId) {
        return ResultVO.ok(jobService.getJobById(jobId));
    }

    @Operation(summary = "获取任务列表")
    @GetMapping("/admin/jobs")
    public ResultVO<PageResultDTO<JobDTO>> listJobs(JobSearchVO jobSearchVO) {
        return ResultVO.ok(jobService.listJobs(jobSearchVO));
    }

    @Operation(summary = "更改任务的状态")
    @PutMapping("/admin/jobs/status")
    public ResultVO<?> updateJobStatus(@RequestBody JobStatusVO jobStatusVO) {
        jobService.updateJobStatus(jobStatusVO);
        return ResultVO.ok();
    }

    @Operation(summary = "执行某个任务")
    @PutMapping("/admin/jobs/run")
    public ResultVO<?> runJob(@RequestBody JobRunVO jobRunVO) {
        jobService.runJob(jobRunVO);
        return ResultVO.ok();
    }

    @Operation(summary = "获取所有job分组")
    @GetMapping("/admin/jobs/jobGroups")
    public ResultVO<List<String>> listJobGroup() {
        return ResultVO.ok(jobService.listJobGroups());
    }
}
