package com.worktrack.infra.web;

import static org.apache.commons.lang3.StringUtils.firstNonBlank;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public class RequestContextLoggingFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(RequestContextLoggingFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_METHOD = "httpMethod";
    private static final String MDC_PATH = "httpPath";
    private static final String MDC_STATUS = "httpStatus";
    private static final String MDC_DURATION = "durationMs";
    private static final String MDC_USER_NAME = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {


        long start = System.nanoTime();

        String correlationId = firstNonBlank(request.getHeader(CORRELATION_ID_HEADER),
                        request.getHeader(REQUEST_ID_HEADER),
                        UUID.randomUUID().toString());

        MDC.put(MDC_REQUEST_ID, correlationId);
        MDC.put(MDC_METHOD, request.getMethod());
        MDC.put(MDC_PATH, request.getRequestURI());
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader(REQUEST_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.put(MDC_STATUS, String.valueOf(response.getStatus()));
            MDC.put(MDC_DURATION, String.valueOf((System.nanoTime() - start) / 1_000_000));

            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                MDC.put(MDC_USER_NAME, auth.getName());
            }

            int status = response.getStatus();
            if (status >= 500) logger.error("request_completed");
            else logger.info("request_completed");

            MDC.clear();
        }
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator/")
                || uri.startsWith("/swagger-ui/")
                || uri.startsWith("/v3/api-docs/");
    }
}
