package com.workshop.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReservationRepository extends JpaRepository<ReservationEntity, String> {
  List<ReservationEntity> findByOrderId(String orderId);
}
