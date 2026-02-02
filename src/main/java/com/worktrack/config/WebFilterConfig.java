package com.worktrack.config;

import com.worktrack.infra.web.RequestContextLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class WebFilterConfig {

    @Bean
    public RequestContextLoggingFilter requestContextLoggingFilter() {
        return new RequestContextLoggingFilter();
    }

    @Bean
    public FilterRegistrationBean<RequestContextLoggingFilter> requestContextLoggingFilterRegistration(
            RequestContextLoggingFilter filter
    ) {
        FilterRegistrationBean<RequestContextLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }
}
