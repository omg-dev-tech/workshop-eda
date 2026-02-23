package com.workshop.fulfillment.api;

import com.workshop.fulfillment.domain.FulfillmentEntity;
import com.workshop.fulfillment.domain.FulfillmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fulfillments")
@RequiredArgsConstructor
public class FulfillmentController {

  private final FulfillmentRepository repo;

  @GetMapping
  public Page<FulfillmentEntity> getAll(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    return repo.findAll(pageable);
  }

  @GetMapping("/{id}")
  public ResponseEntity<FulfillmentEntity> get(@PathVariable Long id) {
    return repo.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{id}/ship")
  public ResponseEntity<FulfillmentEntity> ship(@PathVariable Long id) {
    return repo.findById(id)
        .map(entity -> {
          entity.setStatus("SHIPPED");
          return ResponseEntity.ok(repo.save(entity));
        })
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/healthz")
  public String health() {
    return "fulfillment-service ok";
  }
}

// Made with Bob
