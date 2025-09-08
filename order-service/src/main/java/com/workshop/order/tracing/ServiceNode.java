package com.workshop.order.tracing;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceNode {
  String value();                 // 이 메서드의 가상 서비스명 (예: "vs.taskB")
  Mode mode() default Mode.CLIENT_SERVER; // 보통은 CLIENT→SERVER로 충분
  enum Mode { CLIENT_SERVER, PRODUCER_CONSUMER }
}
