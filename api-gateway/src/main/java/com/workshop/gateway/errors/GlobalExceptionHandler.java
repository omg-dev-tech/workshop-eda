// src/main/java/com/workshop/gateway/errors/GlobalExceptionHandler.java
package com.workshop.gateway.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(RestClientResponseException.class)
  @ResponseStatus(HttpStatus.BAD_GATEWAY)
  public Map<String, Object> backend(RestClientResponseException e) {
    return Map.of(
        "error", "BACKEND_ERROR",
        "status", e.getRawStatusCode(),
        "body", e.getResponseBodyAsString()
    );
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Map<String, Object> generic(Exception e) {
    return Map.of("error", "GATEWAY_ERROR", "message", e.getMessage());
  }
}
