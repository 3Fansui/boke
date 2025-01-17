package com.boke.service;

import com.boke.model.dto.OperationLogDTO;
import com.boke.entity.OperationLog;
import com.boke.model.vo.ConditionVO;
import com.boke.model.dto.PageResultDTO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OperationLogService extends IService<OperationLog> {

    PageResultDTO<OperationLogDTO> listOperationLogs(ConditionVO conditionVO);

}
