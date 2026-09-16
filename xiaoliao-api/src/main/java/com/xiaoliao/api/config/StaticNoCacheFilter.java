package com.xiaoliao.api.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 静态页面/脚本不缓存。
 * 微信/浏览器会缓存旧的 html/js，导致“看作品”打开的是修改前的旧页面；
 * 这里让非接口、非上传资源的页面每次请求都重新拉取。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class StaticNoCacheFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            String uri = request.getRequestURI() == null ? "" : request.getRequestURI();
            boolean noCache = !uri.startsWith("/api/")
                    && !uri.startsWith("/uploads/")
                    && !uri.startsWith("/actuator");
            if (noCache) {
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
            }
        }
        chain.doFilter(req, res);
    }
}
