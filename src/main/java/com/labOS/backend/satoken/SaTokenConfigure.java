package com.labOS.backend.satoken;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.labOS.backend.common.BaseResponse;
import com.labOS.backend.common.ErrorCode;
import com.labOS.backend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 配置类
 * 配置 Sa-Token 的拦截器、过滤器和鉴权规则
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Slf4j
@Configuration
public class SaTokenConfigure {
    
    @Autowired(required = false)
    private V2Config v2Config;

    /** 开放权限的URL路径 - 无需登录即可访问 */
    private final String[] excludePaths = {
            // 静态资源
            "/favicon.ico", 
            "/static/**",
            
            // 认证相关接口（登录、注册）
            // 注意：由于 context-path 是 /api，需要同时配置两种路径
            "/api/auth/**",
            "/auth/**",
            
            // Swagger API 文档
            "/swagger-resources/**",
            "/webjars/**",
            "/v2/**",
            "/v3/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/doc.html",
            
            // 健康检查
            "/actuator/**",
            
            // 前端静态页面
            "/", 
            "/index.html",
            
            // Druid 监控（生产环境建议关闭或加权限）
            "/druid/**"
    };

    /**
     * 注册 Sa-Token 全局过滤器
     * 配置路由拦截规则和认证逻辑
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        // 合并配置文件中的额外排除路径
        List<String> allExcludePaths = new ArrayList<>();
        for (String path : excludePaths) {
            allExcludePaths.add(path);
        }
        
        if (v2Config != null && v2Config.getSaTokenNotFilterUrl() != null) {
            allExcludePaths.addAll(v2Config.getSaTokenNotFilterUrl());
        }
        
        String[] excludeArray = allExcludePaths.toArray(new String[0]);
        log.info("Sa-Token filter exclude paths: {}", allExcludePaths);
        
        return new SaServletFilter()
                // 拦截所有路由
                .addInclude("/**")
                // 排除不需要认证的路由
                .addExclude(excludeArray)
                // 认证函数：每次请求执行（只会对未排除的路径执行）
                .setAuth(obj -> {
                    String path = SaHolder.getRequest().getRequestPath();
                    log.info("=== Auth check started for path: {} ===", path);
                    try {
                        // 检查是否已登录
                        StpUtil.checkLogin();
                        
                        // 获取登录ID
                        String loginId = StpUtil.getLoginIdAsString();
                        if (loginId == null || loginId.isEmpty()) {
                            log.warn("Login ID is null or empty");
                            throw new NotLoginException("User not logged in", "sa-token", "-1");
                        }
                        
                        // 验证用户信息是否存在于 Session 中
                        Object userObj = StpUtil.getSession().get("user");
                        if (userObj == null) {
                            log.warn("User session data missing for loginId: {}", loginId);
                            throw new NotLoginException("User session expired", "sa-token", "-2");
                        }
                        
                        log.info("Auth check passed for user: {}", loginId);
                    } catch (NotLoginException e) {
                        log.warn("Auth check failed: {}", e.getMessage());
                        throw e;
                    }
                })
                // 异常处理函数：认证失败时执行
                .setError(e -> {
                    log.error("Sa-Token filter error: {}", e.getMessage());
                    
                    if (e instanceof NotLoginException) {
                        NotLoginException notLoginException = (NotLoginException) e;
                        BaseResponse<?> response = ResultUtils.error(
                                ErrorCode.NOT_LOGIN_ERROR,
                                "Please login first: " + notLoginException.getMessage()
                        );
                        return JSONUtil.toJsonStr(response);
                    }
                    
                    BaseResponse<?> response = ResultUtils.error(
                            ErrorCode.SYSTEM_ERROR,
                            "Authentication error: " + e.getMessage()
                    );
                    return JSONUtil.toJsonStr(response);
                })
                // 前置函数：每次请求执行认证前执行
                .setBeforeAuth(obj -> {
                    // 获取请求信息用于日志
                    String method = SaHolder.getRequest().getMethod();
                    String path = SaHolder.getRequest().getRequestPath();
                    log.info("=== Incoming Request === Method: {}, Path: {}", method, path);
                    
                    // 设置跨域响应头
                    SaHolder.getResponse()
                            .setHeader("Access-Control-Allow-Origin", "*")
                            .setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                            .setHeader("Access-Control-Allow-Headers", "Content-Type, satoken, Authorization")
                            .setHeader("Access-Control-Max-Age", "3600");
                    
                    // OPTIONS 预检请求直接放行
                    if ("OPTIONS".equals(method)) {
                        log.info("OPTIONS preflight request - allowing");
                        SaRouter.back();
                    }
                });
    }
}
