package com.boke.db.service.impl;

import com.boke.db.mapper.ExceptionLogMapper;
import com.boke.model.dto.ExceptionLogDTO;
import com.boke.db.entity.ExceptionLog;
import com.boke.db.service.ExceptionLogService;
import com.boke.util.BeanCopyUtil;
import com.boke.util.PageUtil;
import com.boke.model.vo.ConditionVO;
import com.boke.model.dto.PageResultDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 异常日志服务实现类
 *
 * 主要功能：
 * 1. 异常日志的分页查询
 * 2. 支持按关键词搜索
 * 3. 按时间倒序排列
 *
 * 技术特点：
 * 1. 继承 MyBatis-Plus 的 ServiceImpl
 * 2. 实现自定义的 ExceptionLogService 接口
 * 3. 使用 Bean 拷贝工具进行对象转换
 *
 * @author boke
 * @version 1.0
 * @since 2024-01-20
 */
@Service
public class ExceptionLogServiceImpl extends ServiceImpl<ExceptionLogMapper, ExceptionLog>
        implements ExceptionLogService {

    /**
     * 分页查询异常日志列表
     *
     * 查询逻辑：
     * 1. 根据条件参数构建查询条件
     * 2. 支持按操作描述模糊搜索
     * 3. 结果按ID降序排列（最新的异常排在前面）
     * 4. 将实体对象转换为DTO返回
     *
     * @param conditionVO 查询条件对象，包含：
     *                    - keywords: 搜索关键词，用于匹配操作描述
     *                    - current: 当前页码（通过 PageUtil 获取）
     *                    - size: 每页大小（通过 PageUtil 获取）
     * @return PageResultDTO<ExceptionLogDTO> 分页结果，包含：
     *         - records: 异常日志DTO列表
     *         - total: 总记录数
     */
    @Override
    public PageResultDTO<ExceptionLogDTO> listExceptionLogs(ConditionVO conditionVO) {
        // 构建分页对象
        Page<ExceptionLog> page = new Page<>(
                PageUtil.getCurrent(),
                PageUtil.getSize());

        // 执行分页查询
        Page<ExceptionLog> exceptionLogPage = this.page(page,
                new LambdaQueryWrapper<ExceptionLog>()
                        // 如果关键词不为空，添加模糊查询条件
                        .like(StringUtils.isNotBlank(conditionVO.getKeywords()),
                                ExceptionLog::getOptDesc,
                                conditionVO.getKeywords())
                        // 按ID降序排序，保证最新的异常日志在前面
                        .orderByDesc(ExceptionLog::getId));

        // 将查询结果转换为DTO对象列表
        List<ExceptionLogDTO> exceptionLogDTOs = BeanCopyUtil.copyList(
                exceptionLogPage.getRecords(),
                ExceptionLogDTO.class);

        // 构建并返回分页结果
        return new PageResultDTO<>(
                exceptionLogDTOs,                    // 异常日志DTO列表
                (int) exceptionLogPage.getTotal());  // 总记录数
    }
}
