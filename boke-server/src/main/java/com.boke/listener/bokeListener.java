package com.boke.listener;

import com.boke.entity.ExceptionLog;
import com.boke.entity.OperationLog;
import com.boke.event.ExceptionLogEvent;
import com.boke.event.OperationLogEvent;
import com.boke.mapper.ExceptionLogMapper;
import com.boke.mapper.OperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class bokeListener {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private ExceptionLogMapper exceptionLogMapper;

    @Async
    @EventListener(OperationLogEvent.class)
    public void saveOperationLog(OperationLogEvent operationLogEvent) {
        operationLogMapper.insert((OperationLog) operationLogEvent.getSource());
    }

    @Async
    @EventListener(ExceptionLogEvent.class)
    public void saveExceptionLog(ExceptionLogEvent exceptionLogEvent) {
        exceptionLogMapper.insert((ExceptionLog) exceptionLogEvent.getSource());
    }

}
