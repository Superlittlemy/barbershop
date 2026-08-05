package com.slm.barbershop.config;

import com.slm.barbershop.utils.TraceContext;
import com.slm.barbershop.utils.TraceIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class TraceFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = TraceIdGenerator.generate();

            // 补充优化：把 traceId 返回给客户端 Header，方便微服务联调与排错
            if (response instanceof HttpServletResponse) {
                ((HttpServletResponse) response).setHeader("X-Trace-Id", traceId);
            }
        }

        TraceContext.setTrace(traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }
}
