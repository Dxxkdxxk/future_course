package com.lzlz.springboot.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;


import java.io.IOException;

public class JwtTokenAuthenticationFilter extends GenericFilterBean {

    private JwtTokenProvider jwtTokenProvider;

    public JwtTokenAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 将获取请求中的token解析出来，进行身份认证和鉴权
 * @param req
 * @param res
 * @param filterChain
     * @return void
     * @author liuzheng
     * @create 2024-07-27
     **/
@Override
public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
        throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    String path = request.getRequestURI();

    // 1. 针对放行路径，直接跳过过滤器逻辑
    if (path.contains("/api/v1/auth/")) {
        filterChain.doFilter(req, res);
        return;
    }

    try {
        String token = jwtTokenProvider.resolveToken(request);
        // 2. 只有当 token 存在时才去验证。如果 token 为空，直接放行，交给 SecurityConfig 处理
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Authentication auth = jwtTokenProvider.getAuthentication(token);
            if (auth != null) {
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
    } catch (Exception e) { 
        // 只有明确的、非法的 Token 尝试才返回 401
        // 或者在这里增加判断：如果是 permitAll 的路径，即使报错也放行
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        return;
    }

    filterChain.doFilter(req, res);
}

}
