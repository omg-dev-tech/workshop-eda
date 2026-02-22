package com.workshop.inventory.api;

import com.workshop.inventory.domain.InventoryEntity;
import com.workshop.inventory.domain.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

  private final InventoryRepository repo;

  @GetMapping
  public List<InventoryEntity> getAll() {
    return repo.findAll();
  }

  @GetMapping("/{sku}")
  public ResponseEntity<InventoryEntity> get(@PathVariable String sku) {
    return repo.findById(sku)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public InventoryEntity create(@RequestBody InventoryEntity entity) {
    return repo.save(entity);
  }

  @PutMapping("/{sku}")
  public ResponseEntity<InventoryEntity> update(
      @PathVariable String sku,
      @RequestBody InventoryEntity entity) {
    return repo.findById(sku)
        .map(existing -> {
          if (entity.getQty() != null) {
            existing.setQty(entity.getQty());
          }
          if (entity.getProductName() != null) {
            existing.setProductName(entity.getProductName());
          }
          return ResponseEntity.ok(repo.save(existing));
        })
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{sku}")
  public ResponseEntity<Void> delete(@PathVariable String sku) {
    if (repo.existsById(sku)) {
      repo.deleteById(sku);
      return ResponseEntity.ok().build();
    }
    return ResponseEntity.notFound().build();
  }
}

// Made with Bob
