package com.worktrack.infra.web;

import static org.apache.commons.lang3.StringUtils.firstNonBlank;

import java.util.UUID;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestContextLoggingInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestContextLoggingInterceptor.class);

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_METHOD = "httpMethod";
    private static final String MDC_PATH = "httpPath";
    private static final String MDC_STATUS = "httpStatus";
    private static final String MDC_DURATION = "durationMs";
    private static final String MDC_USER_NAME = "userId";

    private static final String ATTR_START_TIME_NANOS = RequestContextLoggingInterceptor.class.getName() + ".startNanos";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_START_TIME_NANOS, System.nanoTime());

        String correlationId = firstNonBlank(
                MDC.get(MDC_REQUEST_ID),
                request.getHeader(CORRELATION_ID_HEADER),
                request.getHeader(REQUEST_ID_HEADER),
                UUID.randomUUID().toString()
        );

        MDC.put(MDC_REQUEST_ID, correlationId);
        MDC.put(MDC_METHOD, request.getMethod());
        MDC.put(MDC_PATH, request.getRequestURI());

        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader(REQUEST_ID_HEADER, correlationId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            Long start = (Long) request.getAttribute(ATTR_START_TIME_NANOS);
            long durationMs = start == null ? 0L : (System.nanoTime() - start) / 1_000_000;

            MDC.put(MDC_STATUS, String.valueOf(response.getStatus()));
            MDC.put(MDC_DURATION, String.valueOf(durationMs));

            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                MDC.put(MDC_USER_NAME, auth.getName());
            }

            SpanContext ctx = Span.current().getSpanContext();
            if (ctx.isValid()) {
                MDC.put("traceId", ctx.getTraceId());
                MDC.put("spanId", ctx.getSpanId());
            }

            int status = response.getStatus();
            if (status >= 500) {
                LOGGER.error("request_completed");
            } else {
                LOGGER.info("request_completed");
            }
        } finally {
            MDC.clear();
        }
    }
}
