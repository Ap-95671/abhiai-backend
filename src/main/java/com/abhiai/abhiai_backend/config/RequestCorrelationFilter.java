package com.abhiai.abhiai_backend.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    public static String currentId() {
        String id = MDC.get(REQUEST_ID_MDC_KEY);
        return id == null ? UUID.randomUUID().toString() : id;
    }

    public static Scope scope() {
        boolean created = MDC.get(REQUEST_ID_MDC_KEY) == null;
        if (created) MDC.put(REQUEST_ID_MDC_KEY, UUID.randomUUID().toString());
        return new Scope(created);
    }
    public record Scope(boolean created) implements AutoCloseable {
        @Override public void close() { if (created) MDC.remove(REQUEST_ID_MDC_KEY); }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || !requestId.matches("[A-Za-z0-9_-]{1,64}")) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}
