package com.workshop.order.tracing;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BpEdge {
  String from();
  String to();
  String name();
  int index() default 0;
}