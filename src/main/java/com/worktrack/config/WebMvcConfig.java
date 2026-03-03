package com.worktrack.config;

import com.worktrack.infra.web.RequestContextLoggingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestContextLoggingInterceptor requestContextLoggingInterceptor;

    public WebMvcConfig(RequestContextLoggingInterceptor requestContextLoggingInterceptor) {
        this.requestContextLoggingInterceptor = requestContextLoggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestContextLoggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**");
    }
}
