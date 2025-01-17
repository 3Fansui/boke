package com.boke.interceptor;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boke.util.PageUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;
import java.util.Optional;

import static com.boke.constant.CommonConstant.*;

@Component
@SuppressWarnings("all")
public class PaginationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求参数中获取当前页码
        String currentPage = request.getParameter(CURRENT);

        // 从请求参数中获取页面大小，如果未提供，则使用默认大小
        String pageSize = Optional.ofNullable(request.getParameter(SIZE)).orElse(DEFAULT_SIZE);

        // 如果当前页码不为空并且不为空字符串，则设置分页信息
        if (!Objects.isNull(currentPage) && !StringUtils.isEmpty(currentPage)) {
            PageUtil.setCurrentPage(new Page<>(Long.parseLong(currentPage), Long.parseLong(pageSize)));
        }

        // 返回true，继续处理请求
        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        PageUtil.remove();
    }

}