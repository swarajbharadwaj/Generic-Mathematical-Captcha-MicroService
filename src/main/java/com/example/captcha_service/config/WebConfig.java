package com.example.captcha_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS configuration for the entire application.
 * This is the recommended approach over per-controller annotations.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Apply this policy to all endpoints in the application
            .allowedOrigins("*") // Allow requests from any origin. For production, you should restrict this!
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Explicitly allow OPTIONS for preflight
            .allowedHeaders("*"); // Allow all headers
    }
}
