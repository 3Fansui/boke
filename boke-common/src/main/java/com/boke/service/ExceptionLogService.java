package com.boke.service;

import com.boke.model.dto.ExceptionLogDTO;
import com.boke.entity.ExceptionLog;
import com.boke.model.vo.ConditionVO;
import com.boke.model.dto.PageResultDTO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ExceptionLogService extends IService<ExceptionLog> {

    PageResultDTO<ExceptionLogDTO> listExceptionLogs(ConditionVO conditionVO);

}
