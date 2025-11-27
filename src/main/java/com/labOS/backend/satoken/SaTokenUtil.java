package com.labOS.backend.satoken;

import cn.dev33.satoken.stp.StpUtil;
import com.labOS.backend.model.vo.LoginUserVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 封装 Sa-Token 常用操作
 * 
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Slf4j
public class SaTokenUtil {

	/**
	 * 获取登录用户信息
	 * @return 登录用户VO对象
	 */
	public static LoginUserVO getUser() {
		try {
			if (!StpUtil.isLogin()) {
				return null;
			}
			Object object = StpUtil.getSession().get("user");
			if (object instanceof LoginUserVO) {
				return (LoginUserVO) object;
			}
			return null;
		} catch (Exception e) {
			log.error("Failed to get user from session", e);
			return null;
		}
	}

	/**
	 * 设置用户信息到Session
	 * @param user 登录用户VO对象
	 */
	public static void setUser(LoginUserVO user) {
		if (user == null) {
			throw new IllegalArgumentException("User cannot be null");
		}
		StpUtil.getSession().set("user", user);
		log.info("User set in session: userId={}, userName={}", user.getId(), user.getUserName());
	}

	/**
	 * 获取登录用户ID
	 * @return 用户ID
	 */
	public static Long getUserId() {
		Object loginId = StpUtil.getLoginId();
		if (loginId instanceof Long) {
			return (Long) loginId;
		} else if (loginId instanceof String) {
			return Long.parseLong((String) loginId);
		}
		return null;
	}

	/**
	 * 获取登录用户ID（字符串形式）
	 * @return 用户ID字符串
	 */
	public static String getUserIdAsString() {
		return StpUtil.getLoginIdAsString();
	}

	/**
	 * 获取登录用户名
	 * @return 用户名
	 */
	public static String getUserName() {
		LoginUserVO user = getUser();
		if (user == null) {
			throw new RuntimeException("User not logged in");
		}
		return user.getUserName();
	}

	/**
	 * 获取登录用户角色
	 * @return 用户角色
	 */
	public static String getUserRole() {
		LoginUserVO user = getUser();
		if (user == null) {
			throw new RuntimeException("User not logged in");
		}
		return user.getUserRole();
	}

	/**
	 * 获取登录用户IP
	 * @return IP地址
	 */
	public static String getLoginIp() {
		try {
			return StpUtil.getTokenSession().getString("login_ip");
		} catch (Exception e) {
			log.error("Failed to get login IP", e);
			return null;
		}
	}

	/**
	 * 判断是否已登录
	 * @return 是否已登录
	 */
	public static boolean isLogin() {
		return StpUtil.isLogin();
	}

	/**
	 * 判断是否为管理员
	 * @return 是否为管理员
	 */
	public static boolean isAdmin() {
		LoginUserVO user = getUser();
		return user != null && "admin".equals(user.getUserRole());
	}

	/**
	 * 执行登录
	 * @param userId 用户ID
	 */
	public static void login(Long userId) {
		StpUtil.login(userId);
		log.info("User logged in: userId={}", userId);
	}

	/**
	 * 执行登出
	 */
	public static void logout() {
		Long userId = getUserId();
		StpUtil.logout();
		log.info("User logged out: userId={}", userId);
	}

	/**
	 * 检查是否已登录，未登录则抛出异常
	 */
	public static void checkLogin() {
		StpUtil.checkLogin();
	}

	/**
	 * 获取Token值
	 * @return Token值
	 */
	public static String getTokenValue() {
		return StpUtil.getTokenValue();
	}
}
