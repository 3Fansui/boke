package com.boke.db.service;

import com.boke.model.dto.JobDTO;
import com.boke.db.entity.Job;
import com.boke.model.dto.PageResultDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.boke.model.vo.JobRunVO;
import com.boke.model.vo.JobSearchVO;
import com.boke.model.vo.JobStatusVO;
import com.boke.model.vo.JobVO;

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
