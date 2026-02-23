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
         .allowedHeaders("*") // 모든 헤더 허용
         .exposedHeaders("*") // 모든 헤더 노출
         .allowCredentials(false);
      }
    };
  }
}
