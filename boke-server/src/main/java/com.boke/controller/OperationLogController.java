package com.boke.controller;

import com.boke.annotation.OptLog;
import com.boke.model.dto.OperationLogDTO;
import com.boke.model.vo.ResultVO;
import com.boke.service.OperationLogService;
import com.boke.model.vo.ConditionVO;
import com.boke.model.dto.PageResultDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.boke.constant.OptTypeConstant.DELETE;

@Tag(name =  "操作日志模块")
@RestController
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @Operation(summary =  "查看操作日志")
    @GetMapping("/admin/operation/logs")
    public ResultVO<PageResultDTO<OperationLogDTO>> listOperationLogs(ConditionVO conditionVO) {
        return ResultVO.ok(operationLogService.listOperationLogs(conditionVO));
    }

    @OptLog(optType = DELETE)
    @Operation(summary =  "删除操作日志")
    @DeleteMapping("/admin/operation/logs")
    public ResultVO<?> deleteOperationLogs(@RequestBody List<Integer> operationLogIds) {
        operationLogService.removeByIds(operationLogIds);
        return ResultVO.ok();
    }

}
