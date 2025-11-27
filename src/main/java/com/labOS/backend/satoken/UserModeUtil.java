package com.labOS.backend.satoken;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户登录模式工具类
 * 
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Slf4j
@Component
public class UserModeUtil {
    
    /** 存储用户登录模式的键名 */
    private static final String LOGIN_MODE_KEY = "loginMode";
    
    /**
     * 设置用户登录模式
     * @param loginMode 登录模式
     */
    public static void setLoginMode(LoginMode loginMode) {
        if (!StpUtil.isLogin()) {
            log.warn("Cannot set login mode: user not logged in");
            return;
        }
        StpUtil.getSession().set(LOGIN_MODE_KEY, loginMode);
        log.info("Login mode set: {}", loginMode);
    }
    
    /**
     * 获取当前用户的登录模式
     * @return 登录模式，默认为普通模式
     */
    public static LoginMode getLoginMode() {
        if (!StpUtil.isLogin()) {
            return LoginMode.REGULAR;
        }
        
        try {
            Object mode = StpUtil.getSession().get(LOGIN_MODE_KEY);
            return mode != null ? (LoginMode) mode : LoginMode.REGULAR;
        } catch (Exception e) {
            log.error("Failed to get login mode", e);
            return LoginMode.REGULAR;
        }
    }
    
    /**
     * 清除用户登录模式
     */
    public static void clearLoginMode() {
        if (StpUtil.isLogin()) {
            try {
                StpUtil.getSession().delete(LOGIN_MODE_KEY);
                log.info("Login mode cleared");
            } catch (Exception e) {
                log.error("Failed to clear login mode", e);
            }
        }
    }
    
    /**
     * 判断是否为管理员模式
     * @return 是否为管理员模式
     */
    public static boolean isAdminMode() {
        return LoginMode.ADMIN.equals(getLoginMode());
    }
}