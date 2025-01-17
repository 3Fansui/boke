package com.boke.aspect;

import com.alibaba.fastjson.JSON;
import com.boke.annotation.OptLog;
import com.boke.db.entity.OperationLog;
import com.boke.event.OperationLogEvent;
import com.boke.util.IpUtil;

import com.boke.util.UserUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 操作日志切面类
 * 用于记录系统操作日志，通过AOP方式自动记录带有@OptLog注解的方法的调用信息
 *
 * @author xxx
 * @since xxx
 */
@Aspect
@Component
public class OperationLogAspect {

    /**
     * Spring应用上下文，用于发布事件
     */
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 定义切点
     * 拦截所有带有@OptLog注解的方法
     */
    @Pointcut("@annotation(com.boke.annotation.OptLog)")
    public void operationLogPointCut() {
    }

    /**
     * 在方法返回后记录操作日志
     * 获取请求和方法的相关信息，并发布OperationLogEvent事件
     *
     * @param joinPoint 切点，用于获取方法的信息
     * @param keys 方法的返回值
     */
    @AfterReturning(value = "operationLogPointCut()", returning = "keys")
    @SuppressWarnings("unchecked")
    public void saveOperationLog(JoinPoint joinPoint, Object keys) {
        // 获取请求上下文
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = (HttpServletRequest) Objects.requireNonNull(requestAttributes)
                .resolveReference(RequestAttributes.REFERENCE_REQUEST);

        // 创建操作日志对象
        OperationLog operationLog = new OperationLog();

        // 获取方法的签名信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取API相关注解信息
        Api api = (Api) signature.getDeclaringType().getAnnotation(Api.class);
        ApiOperation apiOperation = method.getAnnotation(ApiOperation.class);
        OptLog optLog = method.getAnnotation(OptLog.class);

        // 设置操作模块、类型和描述
        operationLog.setOptModule(api.tags()[0]);        // 设置操作模块（从Api注解获取）
        operationLog.setOptType(optLog.optType());       // 设置操作类型（从OptLog注解获取）
        operationLog.setOptDesc(apiOperation.value());   // 设置操作描述（从ApiOperation注解获取）

        // 获取并设置请求方法信息
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = method.getName();
        methodName = className + "." + methodName;
        operationLog.setRequestMethod(Objects.requireNonNull(request).getMethod());  // HTTP请求方法
        operationLog.setOptMethod(methodName);                                       // 实际调用的方法

        // 设置请求参数
        if (joinPoint.getArgs().length > 0) {
            // 如果是文件上传，参数直接设置为"file"
            if (joinPoint.getArgs()[0] instanceof MultipartFile) {
                operationLog.setRequestParam("file");
            } else {
                // 否则将参数转换为JSON字符串
                operationLog.setRequestParam(JSON.toJSONString(joinPoint.getArgs()));
            }
        }

        // 设置响应数据
        operationLog.setResponseData(JSON.toJSONString(keys));

        // 设置用户信息
        operationLog.setUserId(UserUtil.getUserDetailsDTO().getId());         // 用户ID
        operationLog.setNickname(UserUtil.getUserDetailsDTO().getNickname()); // 用户昵称

        // 设置IP相关信息
        String ipAddress = IpUtil.getIpAddress(request);
        operationLog.setIpAddress(ipAddress);                    // IP地址
        operationLog.setIpSource(IpUtil.getIpSource(ipAddress)); // IP来源
        operationLog.setOptUri(request.getRequestURI());         // 请求URI

        // 发布操作日志事件
        applicationContext.publishEvent(new OperationLogEvent(operationLog));
    }
}
