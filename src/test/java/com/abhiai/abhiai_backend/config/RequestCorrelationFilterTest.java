package com.abhiai.abhiai_backend.config;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.*;

class RequestCorrelationFilterTest {
    @Test void reusesSafeRequestIdAndRemovesMdcAfterRequest()throws Exception{
        var request=new MockHttpServletRequest();request.addHeader("X-Request-Id","client-request-123");
        var response=new MockHttpServletResponse();
        new RequestCorrelationFilter().doFilter(request,response,(req,res)->assertThat(MDC.get("requestId")).isEqualTo("client-request-123"));
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("client-request-123");assertThat(MDC.get("requestId")).isNull();
    }
    @Test void rejectsLogInjectionInClientCorrelationId()throws Exception{
        var request=new MockHttpServletRequest();request.addHeader("X-Request-Id","forged\nlog-line");
        var response=new MockHttpServletResponse();new RequestCorrelationFilter().doFilter(request,response,(req,res)->{});
        assertThat(response.getHeader("X-Request-Id")).matches("[a-f0-9-]{36}");
    }
}
