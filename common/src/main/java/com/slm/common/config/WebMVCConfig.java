package com.slm.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMVCConfig implements WebMvcConfigurer {

    @Bean
    public com.slm.common.config.PageHandlerMethodArgumentResolver pageHandlerMethodArgumentResolver() {
        return new com.slm.common.config.PageHandlerMethodArgumentResolver();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        WebMvcConfigurer.super.addArgumentResolvers(resolvers);
        resolvers.add(pageHandlerMethodArgumentResolver());
    }

}
