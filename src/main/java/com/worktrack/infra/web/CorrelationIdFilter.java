package com.worktrack.infra.web;

import static org.apache.commons.lang3.StringUtils.firstNonBlank;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String requestId = firstNonBlank(req.getHeader(REQUEST_ID_HEADER), UUID.randomUUID().toString());

        MDC.put(MDC_REQUEST_ID, requestId);
        res.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(MDC_REQUEST_ID);
        }
    }
}
