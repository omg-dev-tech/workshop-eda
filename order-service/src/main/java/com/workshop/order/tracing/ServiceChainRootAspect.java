package com.workshop.order.tracing;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.RequestMapping;

@Aspect
public class ServiceChainRootAspect {
  @Around("@annotation(org.springframework.kafka.annotation.KafkaListener)")
  public Object kafkaRoot(ProceedingJoinPoint pjp) throws Throwable {
    try {
      // 현재 Kafka CONSUMER span context를 루트로 push
      ServiceChainContext.clear();
      ServiceChainContext.set(io.opentelemetry.context.Context.current());
      return pjp.proceed();
    } finally {
      ServiceChainContext.clear();
    }
  }
  


  // HTTP도 체인으로 묶고 싶다면 주석 해제
  @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
  public Object httpRoot(ProceedingJoinPoint pjp) throws Throwable {
    ServiceChainContext.clear();
    ServiceChainContext.set(io.opentelemetry.context.Context.current());
    try { return pjp.proceed(); }
    finally { ServiceChainContext.clear(); }
  }
}
