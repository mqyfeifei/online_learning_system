package com.mqy.olearning.security;

/**
 * 当前登录用户上下文（ThreadLocal）
 * 由 JwtInterceptor 写入，Controller/Service 直接读取
 */
public class UserContext {

    public record LoginUser(Long id, String username, String role) {}

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.id();
    }

    public static String role() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.role();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
