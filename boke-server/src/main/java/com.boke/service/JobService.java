package com.boke.service;

import com.boke.model.dto.JobDTO;
import com.boke.entity.Job;
import com.boke.model.dto.PageResultDTO;
import com.boke.model.vo.*;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface JobService extends IService<Job> {

    void saveJob(JobVO jobVO);

    void updateJob(JobVO jobVO);

    void deleteJobs(List<Integer> tagIds);

    JobDTO getJobById(Integer jobId);

    PageResultDTO<JobDTO> listJobs(JobSearchVO jobSearchVO);

    void updateJobStatus(JobStatusVO jobStatusVO);

    void runJob(JobRunVO jobRunVO);

    List<String> listJobGroups();

}
