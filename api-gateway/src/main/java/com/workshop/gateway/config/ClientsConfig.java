package com.workshop.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientsConfig {

  @Bean
  RestClient orderRestClient(@Value("${order.service-url}") String baseUrl) {
    return RestClient.builder().baseUrl(baseUrl).build();
  }

  @Bean
  RestClient inventoryRestClient(@Value("${inventory.service-url:http://inventory-service:8080}") String baseUrl) {
    return RestClient.builder().baseUrl(baseUrl).build();
  }

  @Bean
  RestClient fulfillmentRestClient(@Value("${fulfillment.service-url:http://fulfillment-service:8080}") String baseUrl) {
    return RestClient.builder().baseUrl(baseUrl).build();
  }

  @Bean
  RestClient analyticsRestClient(@Value("${analytics.service-url:http://analytics-service:8080}") String baseUrl) {
    return RestClient.builder().baseUrl(baseUrl).build();
  }
}
