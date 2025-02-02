package com.boke.config;

import com.boke.filter.JwtAuthenticationTokenFilter;
import com.boke.filter.gitee.GiteeApiClient;
import com.boke.filter.gitee.GiteeAuthenticationFilter;
import com.boke.filter.gitee.GiteeAuthenticationProvider;
import com.boke.handler.AccessDecisionManagerImpl;
import com.boke.handler.FilterInvocationSecurityMetadataSourceImpl;
import com.boke.mapper.UserAuthMapper;
import com.boke.mapper.UserInfoMapper;
import com.boke.mapper.UserRoleMapper;
import com.boke.service.TokenService;
import com.boke.service.impl.UserDetailServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity()
public class WebSecurityConfig {
    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private AccessDeniedHandler accessDeniedHandler;

    @Autowired
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @Autowired
    private AuthenticationFailureHandler authenticationFailureHandler;

    @Autowired
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserAuthMapper userAuthMapper;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private UserDetailServiceImpl userDetailService;
    @Autowired
    private GiteeApiClient giteeApiClient;
    @Bean
    public FilterInvocationSecurityMetadataSource securityMetadataSource() {
        return new FilterInvocationSecurityMetadataSourceImpl();
    }

    @Bean
    public AccessDecisionManager accessDecisionManager() {
        return new AccessDecisionManagerImpl();
    }

    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(daoAuthenticationProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http,HttpServletRequest request) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(t -> {
                    t.loginPage("/users/login")
                            .usernameParameter("username")
                            .passwordParameter("password")
                            .loginProcessingUrl("/users/login")
                            .successHandler(authenticationSuccessHandler)
                            .failureHandler(authenticationFailureHandler);
                })
                .authenticationManager(authenticationManagerBean())
                .authorizeHttpRequests(t -> {
                    t.anyRequest().permitAll();
                })
                .exceptionHandling(t -> {
                    t.accessDeniedHandler(accessDeniedHandler)
                            .authenticationEntryPoint(authenticationEntryPoint);
                })
                .sessionManagement(t -> {
                    t.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(gitee(request), UsernamePasswordAuthenticationFilter.class)
                .build();

       /* http.authorizeRequests()
                .withObjectPostProcessor(new ObjectPostProcessor<FilterSecurityInterceptor>() {
                    @Override
                    public <O extends FilterSecurityInterceptor> O postProcess(O fsi) {
                        fsi.setSecurityMetadataSource(securityMetadataSource());
                        fsi.setAccessDecisionManager(accessDecisionManager());
                        return fsi;
                    }
                })
                .anyRequest().permitAll()
                .and()
                .csrf().disable().exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);*/
    }
    @Bean
    public GiteeAuthenticationFilter gitee(HttpServletRequest request) {
        GiteeAuthenticationProvider provider = new GiteeAuthenticationProvider(userAuthMapper, userInfoMapper, userRoleMapper, userDetailService, request,giteeApiClient);
        GiteeAuthenticationFilter giteeFilter = new GiteeAuthenticationFilter(
                new AntPathRequestMatcher("/users/login/gitee", HttpMethod.POST.name()),
                new ProviderManager(provider),
                authenticationSuccessHandler,
                authenticationFailureHandler, tokenService,objectMapper);
        return giteeFilter;

    }

}
