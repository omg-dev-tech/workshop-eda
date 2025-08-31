package com.workshop.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.payment")
public class PaymentProps {
  private int defaultFailRate = 0; // 0~100
  public int getDefaultFailRate() { return defaultFailRate; }
  public void setDefaultFailRate(int defaultFailRate) { this.defaultFailRate = defaultFailRate; }
}
