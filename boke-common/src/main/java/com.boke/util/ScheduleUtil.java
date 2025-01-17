package com.boke.util;

import com.boke.constant.ScheduleConstant;
import com.boke.db.entity.Job;
import com.boke.enums.JobStatusEnum;
import com.boke.exception.TaskException;

import org.quartz.*;

/**
 * Quartz定时任务工具类
 *
 * @author boke
 * @since 1.0
 */
public class ScheduleUtil {

    /**
     * 获取定时任务的具体执行类
     *
     * @param job 定时任务信息
     * @return 具体执行任务的类
     */
    private static Class<? extends org.quartz.Job> getQuartzJobClass(Job job) {
        // 判断是否允许并发执行
        boolean isConcurrent = Integer.valueOf(1).equals(job.getConcurrent());
        try {
            // 根据是否允许并发执行选择不同的执行器类
            String className = isConcurrent ?
                    "com.boke.quartz.QuartzJobExecution" :  // 允许并发执行的任务类
                    "com.boke.quartz.QuartzDisallowConcurrentExecution";  // 不允许并发执行的任务类

            return (Class<? extends org.quartz.Job>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Job class not found", e);
        }
    }

    /**
     * 获取触发器键
     *
     * @param jobId    任务ID
     * @param jobGroup 任务组名
     * @return 触发器键
     */
    public static TriggerKey getTriggerKey(Integer jobId, String jobGroup) {
        return TriggerKey.triggerKey(ScheduleConstant.TASK_CLASS_NAME + jobId, jobGroup);
    }

    /**
     * 获取任务键
     *
     * @param jobId    任务ID
     * @param jobGroup 任务组名
     * @return 任务键
     */
    public static JobKey getJobKey(Integer jobId, String jobGroup) {
        return JobKey.jobKey(ScheduleConstant.TASK_CLASS_NAME + jobId, jobGroup);
    }

    /**
     * 创建定时任务
     *
     * @param scheduler 调度器
     * @param job      任务信息
     * @throws SchedulerException 调度异常
     * @throws TaskException     任务异常
     */
    public static void createScheduleJob(Scheduler scheduler, Job job) throws SchedulerException, TaskException {
        // 获取任务执行类
        Class<? extends org.quartz.Job> jobClass = getQuartzJobClass(job);
        Integer jobId = job.getId();
        String jobGroup = job.getJobGroup();

        // 构建任务详情
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(getJobKey(jobId, jobGroup)).build();

        // 构建任务触发器
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
        // 处理计划策略
        cronScheduleBuilder = handleCronScheduleMisfirePolicy(job, cronScheduleBuilder);
        // 创建触发器
        CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(getTriggerKey(jobId, jobGroup))
                .withSchedule(cronScheduleBuilder).build();

        // 放入参数
        jobDetail.getJobDataMap().put(ScheduleConstant.TASK_PROPERTIES, job);

        // 如果任务已存在，则删除旧任务
        if (scheduler.checkExists(getJobKey(jobId, jobGroup))) {
            scheduler.deleteJob(getJobKey(jobId, jobGroup));
        }

        // 添加任务
        scheduler.scheduleJob(jobDetail, trigger);

        // 如果任务状态是暂停，则暂停任务
        if (job.getStatus().equals(JobStatusEnum.PAUSE.getValue())) {
            scheduler.pauseJob(ScheduleUtil.getJobKey(jobId, jobGroup));
        }
    }

    /**
     * 处理定时任务的调度策略
     *
     * @param job 任务信息
     * @param cb  CronScheduleBuilder
     * @return 处理后的CronScheduleBuilder
     * @throws TaskException 任务异常
     */
    public static CronScheduleBuilder handleCronScheduleMisfirePolicy(Job job, CronScheduleBuilder cb)
            throws TaskException {
        switch (job.getMisfirePolicy()) {
            case ScheduleConstant.MISFIRE_DEFAULT:  // 默认策略
                return cb;
            case ScheduleConstant.MISFIRE_IGNORE_MISFIRES:  // 忽略错过的触发
                return cb.withMisfireHandlingInstructionIgnoreMisfires();
            case ScheduleConstant.MISFIRE_FIRE_AND_PROCEED:  // 立即触发一次
                return cb.withMisfireHandlingInstructionFireAndProceed();
            case ScheduleConstant.MISFIRE_DO_NOTHING:  // 不触发立即执行
                return cb.withMisfireHandlingInstructionDoNothing();
            default:
                throw new TaskException("The task misfire policy '" + job.getMisfirePolicy()
                        + "' cannot be used in cron schedule tasks", TaskException.Code.CONFIG_ERROR);
        }
    }
}
