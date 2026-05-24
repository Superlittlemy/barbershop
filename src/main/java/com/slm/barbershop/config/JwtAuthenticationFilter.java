package com.slm.barbershop.config;

import com.slm.barbershop.model.AuthUser;
import com.slm.barbershop.utils.JWTUtil;
import com.slm.barbershop.utils.ResponseUtil;
import com.slm.barbershop.utils.UserContext;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter implements Filter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String AUTH_HEADER_TYPE = "Bearer";

    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/auth/login",
            "/auth/register",
            "/auth/send-email-code",
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars",
            "/favicon.ico"
    );

    @Autowired
    private JWTUtil jwtUtil;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String pathWithoutContext = path.substring(contextPath.length());

        if (isExcludedPath(pathWithoutContext)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = httpRequest.getHeader(AUTH_HEADER);
        if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith(AUTH_HEADER_TYPE)) {
            ResponseUtil.setResponse(httpResponse, HttpStatus.UNAUTHORIZED, "未登录");
            return;
        }

        String authToken = authHeader.split(" ")[1];
        log.info("authToken: {}", authToken);

        try {
            Claims claims = jwtUtil.getClaimsFromJwt(authToken);
            Long userId = claims.get("id", Long.class);
            String username = claims.get("username", String.class);

            AuthUser authUser = new AuthUser(userId, username);
            UserContext.setUser(authUser);

            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error("JWT authentication failed", e);
            ResponseUtil.setResponse(httpResponse, HttpStatus.UNAUTHORIZED, "无效token");
        } finally {
            UserContext.clear();
        }
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDE_PATHS.stream().anyMatch(path::startsWith);
    }

}