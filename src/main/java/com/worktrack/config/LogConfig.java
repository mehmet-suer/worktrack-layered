package com.worktrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class LogConfig {

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(false);
        filter.setMaxPayloadLength(10000); // body'den maksimum kaç karakter loglansın
        filter.setIncludeHeaders(false);   // header bilgisi de eklensin mi
        filter.setIncludeClientInfo(true);
        filter.setAfterMessagePrefix("REQUEST DATA: ");
        return filter;
    }
}
