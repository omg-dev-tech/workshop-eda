package com.workshop.payment.api;

import com.workshop.payment.config.PaymentProps;
import com.workshop.payment.model.PaymentRequest;
import com.workshop.payment.model.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

  private final PaymentProps props;
  private final SecureRandom rnd = new SecureRandom();

  @PostMapping("/authorize")
  public PaymentResponse authorize(
      @Valid @RequestBody PaymentRequest req,
      @RequestHeader(value = "X-Force-Payment", required = false) String forceHeader,
      @RequestParam(value = "force", required = false) String forceParam
  ) {
    String force = (forceHeader != null && !forceHeader.isBlank()) ? forceHeader : forceParam;

    boolean authorized;
    String reason = null;

    if ("success".equalsIgnoreCase(force)) {
      authorized = true;
    } else if ("fail".equalsIgnoreCase(force)) {
      authorized = false; reason = "FORCED_FAIL";
    } else if (req.amount() <= 0) {
      authorized = false; reason = "INVALID_AMOUNT";
    } else {
      int failRate = Math.max(0, Math.min(100, props.getDefaultFailRate()));
      authorized = rnd.nextInt(100) >= failRate;
      if (!authorized) reason = "RANDOM_DECLINE";
    }

    String authId = authorized ? "pay-" + UUID.randomUUID() : null;
    var res = new PaymentResponse(
        authorized ? "AUTHORIZED" : "DECLINED",
        authId,
        reason,
        System.currentTimeMillis()
    );

    log.info("AUTH {} orderId={} amount={} {}, items={}",
        res.status(), req.orderId(), req.amount(), req.currency(),
        req.items() == null ? 0 : req.items().size());

    return res;
  }
}
