package com.workshop.order.tracing;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceNode {
  /** 이 메서드를 서비스 레벨로 승격할 때 사용할 이름 (예: "vs.taskB") */
  String value();

  /** 필요 시 메시징 모델로 표현하고 싶을 때 선택 */
  Mode mode() default Mode.CLIENT_SERVER;
  enum Mode { CLIENT_SERVER, PRODUCER_CONSUMER }
}
