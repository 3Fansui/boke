package com.boke.filter.gitee;

import com.boke.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.AuthenticationManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import java.io.IOException;
import java.util.stream.Collectors;

@Slf4j
public class GiteeAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
  private ObjectMapper objectMapper;
  private TokenService tokenService;
  public GiteeAuthenticationFilter(AntPathRequestMatcher pathRequestMatcher,
                                   AuthenticationManager authenticationManager,
                                   AuthenticationSuccessHandler authenticationSuccessHandler,
                                   AuthenticationFailureHandler authenticationFailureHandler,
                                   TokenService tokenService,
                                   ObjectMapper objectMapper) {
    super(pathRequestMatcher);
    setAuthenticationManager(authenticationManager);
    setAuthenticationSuccessHandler(authenticationSuccessHandler);
    setAuthenticationFailureHandler(authenticationFailureHandler);
    this.tokenService = tokenService;
    this.objectMapper = objectMapper;
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request,
                                              HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
    logger.debug("use GiteeAuthenticationFilter");

    // 提取请求数据 http://localhost:8081/?code=e08d80cc15033661f291d7b35ea8dc2ae273d3aa9f309a87f88b4dfeb950bb3e
    String requestJsonData = request.getReader().lines()
        .collect(Collectors.joining(System.lineSeparator()));
    System.out.println("授权码响应"+requestJsonData);

    String code = requestJsonData;

    GiteeAuthentication authentication = new GiteeAuthentication();
    authentication.setCode(code);
    authentication.setAuthenticated(false); // 提取参数阶段，authenticated一定是false
    return this.getAuthenticationManager().authenticate(authentication);
  }

  /**
   * 认证成功，需要生成jwt令牌, 返回给前端
   */
  /*@Override
  protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
    // 从请求属性中获取用户名
    String username = (String) request.getAttribute("username");

    UserInfoDTO userLoginDTO = BeanCopyUtil.copyObject(UserUtil.getUserDetailsDTO(), UserInfoDTO.class);
    if (Objects.nonNull(authResult)) {
      UserDetailsDTO userDetailsDTO = (UserDetailsDTO) authResult.getPrincipal();
      System.out.println("---"+userDetailsDTO);
      String token = tokenService.createToken(userDetailsDTO);
      userLoginDTO.setToken(token);
    }
    response.setContentType(CommonConstant.APPLICATION_JSON);
    response.getWriter().write(JSON.toJSONString(ResultVO.ok(userLoginDTO)));
  }*/
}