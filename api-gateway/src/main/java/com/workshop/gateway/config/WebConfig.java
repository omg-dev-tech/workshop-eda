package com.workshop.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
  
  @Value("${cors.allowed-origins:http://localhost:3000}")
  private String allowedOrigins;
  
  @Bean
  public WebMvcConfigurer crsConfigurer() {
    return new WebMvcConfigurer() {
      @Override public void addCorsMappings(CorsRegistry r) {
        r.addMapping("/**")
         .allowedOrigins(allowedOrigins.split(","))
         .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
         .allowedHeaders("Content-Type","Authorization",
                         "X-INSTANA-T","X-INSTANA-S","X-INSTANA-L") // ← 중요
         .exposedHeaders("Server-Timing") // ← 브라우저가 읽을 수 있게
         .allowCredentials(false);
      }
    };
  }
}
