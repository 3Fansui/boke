package com.boke.db.service.impl;

import com.boke.db.mapper.JobMapper;
import com.boke.enums.JobStatusEnum;
import com.boke.model.dto.JobDTO;
import com.boke.db.entity.Job;
import com.boke.model.dto.PageResultDTO;
import com.boke.db.service.JobService;
import com.boke.model.vo.JobRunVO;
import com.boke.model.vo.JobSearchVO;
import com.boke.model.vo.JobStatusVO;
import com.boke.model.vo.JobVO;

import com.boke.util.BeanCopyUtil;
import com.boke.util.CronUtil;
import com.boke.util.PageUtil;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boke.util.ScheduleUtil;
import lombok.SneakyThrows;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 定时任务服务实现类
 *
 * @author boke
 * @since 1.0
 */
@Service
public class JobServiceImpl extends ServiceImpl<JobMapper, Job> implements JobService {

    /**
     * 定时任务调度器
     */
    @Autowired
    private Scheduler scheduler;

    /**
     * 定时任务数据访问层
     */
    @Autowired
    private JobMapper jobMapper;

    /**
     * 项目启动时初始化定时任务
     * 先清空所有定时任务，然后从数据库中加载定时任务并重新创建
     *
     * @throws SchedulerException 调度器异常
     */
    @SneakyThrows  // Lombok注解，自动处理受检异常
    @PostConstruct // 在依赖注入完成后自动执行该方法
    public void init() {
        // 清空调度器中的所有定时任务
        scheduler.clear();
        // 从数据库中获取所有定时任务
        List<Job> jobs = jobMapper.selectList(null);
        // 遍历任务列表，重新创建定时任务
        for (Job job : jobs) {
            ScheduleUtil.createScheduleJob(scheduler, job);
        }
    }

    /**
     * 保存定时任务
     *
     * @param jobVO 定时任务VO对象
     * @throws SchedulerException 调度器异常
     * @throws Exception 校验异常
     */
    @SneakyThrows  // Lombok注解，自动处理受检异常
    @Transactional(rollbackFor = Exception.class)  // 开启事务，任何异常都回滚
    public void saveJob(JobVO jobVO) {
        // 校验Cron表达式是否有效
        checkCronIsValid(jobVO);

        // 将VO对象转换为实体对象
        Job job = BeanCopyUtil.copyObject(jobVO, Job.class);

        // 将任务信息插入数据库
        int row = jobMapper.insert(job);

        // 如果数据库插入成功，则创建定时任务
        if (row > 0) {
            ScheduleUtil.createScheduleJob(scheduler, job);
        }
    }

    /**
     * 更新定时任务
     *
     * @param jobVO 定时任务VO对象
     * @throws SchedulerException 调度器异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)  // 开启事务，任何异常都回滚
    public void updateJob(JobVO jobVO) {
        // 校验Cron表达式是否有效
        checkCronIsValid(jobVO);

        // 查询原有任务信息
        Job temp = jobMapper.selectById(jobVO.getId());

        // 将新的任务信息转换为实体对象
        Job job = BeanCopyUtil.copyObject(jobVO, Job.class);

        // 更新数据库中的任务信息
        int row = jobMapper.updateById(job);

        // 如果数据库更新成功，则更新调度器中的任务
        if (row > 0) {
            updateSchedulerJob(job, temp.getJobGroup());
        }
    }

    /**
     * 批量删除定时任务
     *
     * @param tagIds 需要删除的任务ID列表
     * @throws RuntimeException 删除失败时抛出运行时异常
     */
    @SneakyThrows  // Lombok注解，自动处理受检异常
    @Override
    @Transactional(rollbackFor = Exception.class)  // 开启事务，任何异常都回滚
    public void deleteJobs(List<Integer> tagIds) {
        // 查询要删除的任务信息列表
        // 需要先查询出来是因为删除后将无法获取任务组信息
        List<Job> jobs = jobMapper.selectList(new LambdaQueryWrapper<Job>()
                .in(Job::getId, tagIds));  // 使用in条件查询指定ID的任务

        // 从数据库中删除任务记录
        int row = jobMapper.delete(new LambdaQueryWrapper<Job>()
                .in(Job::getId, tagIds));  // 使用in条件批量删除

        // 如果数据库删除成功，则从调度器中删除相应的任务
        if (row > 0) {
            // 遍历所有需要删除的任务
            jobs.forEach(item -> {
                try {
                    // 从调度器中删除任务
                    // 通过任务ID和任务组构建任务键进行删除
                    scheduler.deleteJob(ScheduleUtil.getJobKey(item.getId(), item.getJobGroup()));
                } catch (SchedulerException e) {
                    // 如果删除失败，将SchedulerException转换为RuntimeException抛出
                    // 这将触发事务回滚
                    throw new RuntimeException(e);
                }
            });
        }
    }

    /**
     * Cron表达式格式：秒 分 时 日 月 周 年(可选)
     *
     * 字段说明：
     * 秒（0-59）
     * 分（0-59）
     * 时（0-23）
     * 日（1-31）
     * 月（1-12 或 JAN-DEC）
     * 周（1-7 或 SUN-SAT）
     * 年（可选，1970-2099）
     */

    //   "0 0 12 * * ?"      // 每天中午12点触发
    //   "0 15 10 ? * *"     // 每天上午10:15触发
    //   "0 15 10 * * ?"     // 每天上午10:15触发
    //   "0 15 10 * * ? *"   // 每天上午10:15触发
    //   "0 */5 * * * ?"     // 每隔5分钟触发一次
    //   "0 0 12 ? * WED"    // 每个星期三中午12点触发


    /**
     * 根据任务ID获取定时任务详情
     *
     * @param jobId 任务ID
     * @return 任务详情DTO，包含下次执行时间
     */
    @Override
    public JobDTO getJobById(Integer jobId) {
        // 根据ID查询任务基本信息
        Job job = jobMapper.selectById(jobId);

        // 将实体对象转换为DTO对象
        JobDTO jobDTO = BeanCopyUtil.copyObject(job, JobDTO.class);

        // 根据Cron表达式计算下次执行时间
        Date nextExecution = CronUtil.getNextExecution(jobDTO.getCronExpression());

        // 设置下次执行时间
        jobDTO.setNextValidTime(nextExecution);

        return jobDTO;
    }



    /**
     * 分页查询定时任务列表
     *
     * @param jobSearchVO 任务查询条件
     * @return 分页任务列表
     */
    @SneakyThrows  // Lombok注解，自动处理受检异常（主要是处理CompletableFuture的异常）
    @Override
    public PageResultDTO<JobDTO> listJobs(JobSearchVO jobSearchVO) {
        // 异步查询总数
        // 使用CompletableFuture提高查询效率，让count查询与数据查询并行执行
        CompletableFuture<Integer> asyncCount = CompletableFuture.supplyAsync(() ->
                jobMapper.countJobs(jobSearchVO)
        );

        // 查询任务数据
        // 使用PageUtil工具类获取分页参数（当前页码和每页大小）
        List<JobDTO> jobDTOs = jobMapper.listJobs(
                PageUtil.getLimitCurrent(),  // 获取当前页的起始位置
                PageUtil.getSize(),          // 获取每页显示条数
                jobSearchVO                  // 查询条件
        );

        // 构建分页结果
        // asyncCount.get() 获取异步查询的总数结果
        return new PageResultDTO<>(jobDTOs, asyncCount.get());
    }


    /**
     * 更新定时任务的运行状态
     *
     * @param jobStatusVO 任务状态信息（包含任务ID和目标状态）
     * @throws SchedulerException 调度器异常
     */
    @SneakyThrows  // Lombok注解，自动处理调度器可能抛出的异常
    @Override
    public void updateJobStatus(JobStatusVO jobStatusVO) {
        // 查询原任务信息
        Job job = jobMapper.selectById(jobStatusVO.getId());

        // 如果状态相同，无需更新
        if (job.getStatus().equals(jobStatusVO.getStatus())) {
            return;
        }

        // 获取需要更新的状态和任务信息
        Integer status = jobStatusVO.getStatus();
        Integer jobId = job.getId();
        String jobGroup = job.getJobGroup();

        // 构建更新条件
        LambdaUpdateWrapper<Job> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Job::getId, jobStatusVO.getId())  // 根据ID更新
                .set(Job::getStatus, status);          // 设置新状态

        // 更新数据库中的任务状态
        int row = jobMapper.update(null, updateWrapper);

        // 如果数据库更新成功，则更新调度器中的任务状态
        if (row > 0) {
            if (JobStatusEnum.NORMAL.getValue().equals(status)) {
                // 如果目标状态是正常，恢复任务
                scheduler.resumeJob(ScheduleUtil.getJobKey(jobId, jobGroup));
            } else if (JobStatusEnum.PAUSE.getValue().equals(status)) {
                // 如果目标状态是暂停，暂停任务
                scheduler.pauseJob(ScheduleUtil.getJobKey(jobId, jobGroup));
            }
        }
    }

    /**
     * 立即执行一次定时任务
     *
     * @param jobRunVO 任务运行信息（包含任务ID和任务组）
     * @throws SchedulerException 调度器异常
     */
    @SneakyThrows  // Lombok注解，自动处理调度器可能抛出的异常
    @Override
    public void runJob(JobRunVO jobRunVO) {
        // 获取任务ID和任务组信息
        Integer jobId = jobRunVO.getId();
        String jobGroup = jobRunVO.getJobGroup();

        // 通过调度器立即触发任务执行
        // 这里不会影响原有的调度计划，只是额外执行一次任务
        scheduler.triggerJob(ScheduleUtil.getJobKey(jobId, jobGroup));
    }

    /**
     * 获取所有任务组名称列表
     *
     * @return 任务组名称列表
     */
    @Override
    public List<String> listJobGroups() {
        // 调用数据访问层获取所有任务组
        return jobMapper.listJobGroups();
    }

    /**
     * 校验Cron表达式是否有效
     *
     * @param jobVO 任务信息对象
     * @throws IllegalArgumentException 当Cron表达式无效时抛出异常
     */
    private void checkCronIsValid(JobVO jobVO) {
        // 使用CronUtil工具类验证表达式的有效性
        boolean valid = CronUtil.isValid(jobVO.getCronExpression());
        // 使用断言，如果表达式无效则抛出异常
        Assert.isTrue(valid, "Cron表达式无效!");
    }

    /**
     * 更新调度器中的定时任务
     * 采用先删除再创建的方式，确保任务更新的原子性
     *
     * @param job 任务信息
     * @param jobGroup 任务组名称
     * @throws SchedulerException 调度器异常
     */
    @SneakyThrows  // Lombok注解，自动处理调度器可能抛出的异常
    public void updateSchedulerJob(Job job, String jobGroup) {
        // 获取任务ID
        Integer jobId = job.getId();
        // 构建任务键
        JobKey jobKey = ScheduleUtil.getJobKey(jobId, jobGroup);

        // 如果任务已存在，则先删除
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }

        // 重新创建任务
        ScheduleUtil.createScheduleJob(scheduler, job);
    }

}
