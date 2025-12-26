package com.labOS.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS Configuration
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:https://ai4labos.com,https://www.ai4labos.com,http://localhost:8080,http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Value("${cors.enable-all-origins:false}")
    private boolean enableAllOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // covers every requests
        CorsRegistration corsRegistration = registry.addMapping("/**")
                // permitted to send Cookie
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*");

        // In development, allow all origins if enabled, otherwise use configured origins
        if (enableAllOrigins) {
            corsRegistration.allowedOriginPatterns("*");
        } else {
            // Split comma-separated origins and add them
            String[] origins = java.util.Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
            corsRegistration.allowedOriginPatterns(origins);
        }
    }
}
