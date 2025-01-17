package com.boke.controller;

import com.boke.exception.BizException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;




@Api(tags = "异常处理模块")
@RestController
public class BizExceptionController {

    /**
     * 处理BizException
     * 该方法从请求中获取BizException对象并抛出。如果获取的对象不是BizException类型，则抛出一般异常。
     *
     * @param request HTTP请求对象，用于获取存储在请求属性中的异常对象
     * @throws BizException 当请求属性中包含BizException对象时抛出
     * @throws Exception 当请求属性中不包含BizException对象时抛出
     */
    @SneakyThrows
    @ApiOperation("/处理BizException")
    @RequestMapping("/bizException")
    public void handleBizException(HttpServletRequest request) {
        // 检查请求属性 "bizException" 是否为 BizException 类型的实例
        if (request.getAttribute("bizException") instanceof BizException) {
            System.out.println(request.getAttribute("bizException"));
            // 抛出BizException
            throw ((BizException) request.getAttribute("bizException"));
        } else {
            // 抛出一般异常
            throw new Exception();
        }
    }

}
