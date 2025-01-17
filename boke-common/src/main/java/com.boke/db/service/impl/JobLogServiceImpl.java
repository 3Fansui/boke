package com.boke.db.service.impl;

import com.boke.db.mapper.JobLogMapper;
import com.boke.model.dto.JobLogDTO;
import com.boke.db.entity.JobLog;
import com.boke.db.service.JobLogService;
import com.boke.util.BeanCopyUtil;
import com.boke.util.PageUtil;
import com.boke.model.vo.JobLogSearchVO;
import com.boke.model.dto.PageResultDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 定时任务日志服务实现类
 *
 * @author boke
 * @since 1.0
 */
@Service
public class JobLogServiceImpl extends ServiceImpl<JobLogMapper, JobLog> implements JobLogService {

    /**
     * 定时任务日志数据访问层
     */
    @Autowired
    private JobLogMapper jobLogMapper;

    /**
     * 分页查询定时任务日志
     *
     * @param jobLogSearchVO 查询条件对象
     * @return 分页结果，包含日志详情列表
     */
    @SneakyThrows
    @Override
    public PageResultDTO<JobLogDTO> listJobLogs(JobLogSearchVO jobLogSearchVO) {
        // 构建查询条件
        LambdaQueryWrapper<JobLog> queryWrapper = new LambdaQueryWrapper<JobLog>()
                // 按创建时间降序排序
                .orderByDesc(JobLog::getCreateTime)
                // 根据任务ID查询
                .eq(Objects.nonNull(jobLogSearchVO.getJobId()),
                        JobLog::getJobId, jobLogSearchVO.getJobId())
                // 根据任务名称模糊查询
                .like(StringUtils.isNotBlank(jobLogSearchVO.getJobName()),
                        JobLog::getJobName, jobLogSearchVO.getJobName())
                // 根据任务组名模糊查询
                .like(StringUtils.isNotBlank(jobLogSearchVO.getJobGroup()),
                        JobLog::getJobGroup, jobLogSearchVO.getJobGroup())
                // 根据执行状态查询
                .eq(Objects.nonNull(jobLogSearchVO.getStatus()),
                        JobLog::getStatus, jobLogSearchVO.getStatus())
                // 根据时间范围查询
                .between(Objects.nonNull(jobLogSearchVO.getStartTime()) &&
                                Objects.nonNull(jobLogSearchVO.getEndTime()),
                        JobLog::getCreateTime,
                        jobLogSearchVO.getStartTime(),
                        jobLogSearchVO.getEndTime());

        // 创建分页对象
        Page<JobLog> page = new Page<>(PageUtil.getCurrent(), PageUtil.getSize());
        // 执行分页查询
        Page<JobLog> jobLogPage = jobLogMapper.selectPage(page, queryWrapper);
        // 转换为DTO对象列表
        List<JobLogDTO> jobLogDTOs = BeanCopyUtil.copyList(jobLogPage.getRecords(), JobLogDTO.class);
        // 返回分页结果
        return new PageResultDTO<>(jobLogDTOs, (int)jobLogPage.getTotal());
    }

    /**
     * 批量删除定时任务日志
     *
     * @param ids 需要删除的日志ID列表
     */
    @Override
    public void deleteJobLogs(List<Integer> ids) {
        // 构建删除条件
        LambdaQueryWrapper<JobLog> queryWrapper = new LambdaQueryWrapper<JobLog>()
                .in(JobLog::getId, ids);
        // 执行批量删除
        jobLogMapper.delete(queryWrapper);
    }

    /**
     * 清空所有定时任务日志
     */
    @Override
    public void cleanJobLogs() {
        // 传入null表示删除所有记录
        jobLogMapper.delete(null);
    }

    /**
     * 获取所有任务日志的分组名称
     *
     * @return 任务组名称列表
     */
    @Override
    public List<String> listJobLogGroups() {
        return jobLogMapper.listJobLogGroups();
    }
}
