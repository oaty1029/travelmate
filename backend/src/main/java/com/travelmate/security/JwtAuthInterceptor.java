package com.travelmate.security;

import com.travelmate.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        // 论坛 GET 请求公开放行（浏览帖子列表/详情/评论无需登录）
        // 但 POST/PUT/DELETE 仍需认证（发帖/评论/点赞/删除）；通知接口需要登录
        String uri = request.getRequestURI();
        if ("GET".equalsIgnoreCase(request.getMethod()) && uri.startsWith("/api/v1/forum/")
                && !uri.startsWith("/api/v1/forum/notifications")) {
            // 可选认证：带合法 token 时解析出当前用户，便于详情接口返回“是否已点赞”
            String optionalToken = resolveToken(request);
            if (optionalToken != null) {
                try {
                    if (jwtUtil.validate(optionalToken)) {
                        request.setAttribute("currentUserId", jwtUtil.getUserId(optionalToken));
                        request.setAttribute("currentUserRole", jwtUtil.getRole(optionalToken));
                    }
                } catch (Exception e) {
                    // token 无效按未登录处理
                }
            }
            return true;
        }

        String token = resolveToken(request);
        if (token == null) {
            sendError(response, 401, "请先登录");
            return false;
        }
        try {
            if (jwtUtil.validate(token)) {
                Long userId = jwtUtil.getUserId(token);
                String role = jwtUtil.getRole(token);
                request.setAttribute("currentUserId", userId);
                request.setAttribute("currentUserRole", role);
                return true;
            }
        } catch (Exception e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
        }
        sendError(response, 401, "登录已过期，请重新登录");
        return false;
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private void sendError(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, msg)));
    }
}
