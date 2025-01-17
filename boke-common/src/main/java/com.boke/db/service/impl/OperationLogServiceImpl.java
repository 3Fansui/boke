package com.boke.db.service.impl;

import com.boke.db.mapper.OperationLogMapper;
import com.boke.model.dto.OperationLogDTO;
import com.boke.db.entity.OperationLog;
import com.boke.db.service.OperationLogService;
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
 * 操作日志服务实现类
 * 处理系统操作日志的查询等相关业务逻辑
 *
 * @author boke
 * @since 1.0
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    /**
     * 分页查询操作日志列表
     * 支持按模块名称和操作描述进行模糊搜索
     *
     * @param conditionVO 查询条件，包含关键词搜索
     * @return 分页结果，包含操作日志详情列表
     */
    @Override
    public PageResultDTO<OperationLogDTO> listOperationLogs(ConditionVO conditionVO) {
        // 创建分页对象
        Page<OperationLog> page = new Page<>(
                PageUtil.getCurrent(),  // 获取当前页码
                PageUtil.getSize()      // 获取每页显示条数
        );

        // 执行分页查询
        Page<OperationLog> operationLogPage = this.page(page, new LambdaQueryWrapper<OperationLog>()
                // 根据关键词模糊匹配模块名称
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()),
                        OperationLog::getOptModule,
                        conditionVO.getKeywords())
                // 或者模糊匹配操作描述
                .or()
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()),
                        OperationLog::getOptDesc,
                        conditionVO.getKeywords())
                // 按ID降序排序，最新的操作日志显示在前面
                .orderByDesc(OperationLog::getId));

        // 将实体对象转换为DTO对象
        List<OperationLogDTO> operationLogDTOs = BeanCopyUtil.copyList(
                operationLogPage.getRecords(),  // 获取当前页的数据记录
                OperationLogDTO.class           // 目标DTO类
        );

        // 返回分页结果
        return new PageResultDTO<>(
                operationLogDTOs,                    // 转换后的DTO列表
                (int) operationLogPage.getTotal()    // 总记录数
        );
    }
}
