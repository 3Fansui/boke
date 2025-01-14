package com.boke.handler;

import com.alibaba.fastjson.JSON;
import com.boke.constant.CommonConstant;
import com.boke.model.vo.ResultVO;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录失败处理
 */
@Component
public class AuthenticationFailHandlerImpl implements AuthenticationFailureHandler {


    @Override
    public void onAuthenticationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException {
        httpServletResponse.setContentType(CommonConstant.APPLICATION_JSON);
        httpServletResponse.getWriter().write(JSON.toJSONString(ResultVO.fail(e.getMessage())));
    }

}
