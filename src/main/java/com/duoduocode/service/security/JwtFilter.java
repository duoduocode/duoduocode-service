package com.duoduocode.service.security;

import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT 认证过滤器
 * 拦截请求，验证 Token，将用户ID存入 SecurityContext
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    /**
     * 不需要认证的路径
     */
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/v1/auth/login",
            "/v1/auth/dev-login",
            "/v1/auth/refresh-token",
            "/doc.html",
            "/swagger-resources",
            "/v2/api-docs",
            "/v3/api-docs",
            "/webjars/**",
            "/favicon.ico",
            "/error"
    );

    private final PathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // OPTIONS 预检请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return EXCLUDE_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 从请求头中获取 token
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "Token缺失，请先登录");
            return;
        }

        String token = authHeader.substring(7);

        // 验证 Token
        if (!jwtUtils.validateToken(token)) {
            if (jwtUtils.isTokenExpired(token)) {
                sendUnauthorized(response, "Token已过期，请重新登录");
            } else {
                sendUnauthorized(response, "Token无效");
            }
            return;
        }

        // 解析用户ID
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            sendUnauthorized(response, "Token无效，无法解析用户信息");
            return;
        }

        // 将用户ID存入 SecurityContext
        SecurityContext.setUserId(userId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContext.clear();
        }
    }

    /**
     * 返回 401 未授权响应
     */
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", ResultCode.UNAUTHORIZED.getCode());
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
