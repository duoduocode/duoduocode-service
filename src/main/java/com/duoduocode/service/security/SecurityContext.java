package com.duoduocode.service.security;

/**
 * 安全上下文
 * 用于存储当前请求的用户信息
 *
 * @author duoduo
 */
public class SecurityContext {

    /**
     * 使用 ThreadLocal 存储当前用户ID
     */
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，未登录返回null
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 设置当前用户ID
     *
     * @param userId 用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 清除上下文
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }

    /**
     * 判断当前用户是否已登录
     *
     * @return 是否已登录
     */
    public static boolean isAuthenticated() {
        return getUserId() != null;
    }

    /**
     * 获取当前用户ID，未登录抛出异常
     *
     * @return 用户ID
     * @throws IllegalStateException 未登录时抛出
     */
    public static Long requireUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new IllegalStateException("用户未登录");
        }
        return userId;
    }

}
