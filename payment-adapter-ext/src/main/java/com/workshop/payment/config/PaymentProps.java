package com.workshop.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.payment")
public class PaymentProps {
  private int defaultFailRate = 0; // 0~100
  private boolean errorMode = false;
  public int getDefaultFailRate() { return defaultFailRate; }
  public void setDefaultFailRate(int defaultFailRate) { this.defaultFailRate = defaultFailRate; }
  public boolean getErrorMode() { return errorMode; }
  public void setErrorMode(boolean errorMode) { this.errorMode = errorMode; } 
}
