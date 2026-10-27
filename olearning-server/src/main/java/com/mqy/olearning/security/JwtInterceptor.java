package com.mqy.olearning.security;

import com.mqy.olearning.common.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * JWT 拦截器：
 * 1. 校验 token 合法性
 * 2. 把登录用户放进 UserContext
 * 3. 校验 @RequireRole 角色权限（越权返回 403）
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String header = request.getHeader("Authorization");
        // 浏览器直接下载/打开附件时（window.open）带不了请求头，允许用 ?token= 兜底
        if (!StringUtils.hasText(header)) {
            String queryToken = request.getParameter("token");
            if (StringUtils.hasText(queryToken)) {
                header = "Bearer " + queryToken;
            }
        }

        // 公开接口「尽力解析」：带了合法 token 就把用户放进上下文，
        // 没带或过期也照常放行。这样课程详情这类接口在登录后能返回
        // 已选状态与学习进度，未登录时退化成游客视图，而不是直接 401。
        if (request.getRequestURI().startsWith("/api/public/")) {
            tryParseInto(header);
            return true;
        }

        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或登录已过期，请重新登录");
        }

        Claims claims;
        try {
            claims = jwtUtil.parse(header.substring(7));
        } catch (Exception e) {
            throw new BusinessException(401, "登录状态无效，请重新登录");
        }

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);
        UserContext.set(new UserContext.LoginUser(userId, username, role));

        // 角色校验：方法上的注解优先，其次类上的
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole != null && Arrays.stream(requireRole.value()).noneMatch(role::equals)) {
            throw new BusinessException(403, "无权限访问该接口");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    /** 尽力解析：token 合法就写入上下文，任何异常都静默忽略 */
    private void tryParseInto(String header) {
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            return;
        }
        try {
            Claims claims = jwtUtil.parse(header.substring(7));
            UserContext.set(new UserContext.LoginUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("role", String.class)));
        } catch (Exception ignored) {
            // 公开接口不做鉴权，token 有问题就当游客
        }
    }
}
