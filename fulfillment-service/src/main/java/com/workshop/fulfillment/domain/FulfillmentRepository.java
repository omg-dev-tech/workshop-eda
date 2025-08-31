package com.workshop.fulfillment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FulfillmentRepository extends JpaRepository<FulfillmentEntity, Long> {
  Optional<FulfillmentEntity> findByOrderId(String orderId);
}
