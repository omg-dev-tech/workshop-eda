package com.workshop.gateway.api;

import com.workshop.gateway.model.FulfillmentView;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminGatewayController {

  private final RestClient inventoryRestClient;
  private final RestClient fulfillmentRestClient;
  private final RestClient analyticsRestClient;

  // Inventory APIs
  @GetMapping("/inventory")
  public ResponseEntity<?> getAllInventory() {
    return ResponseEntity.ok(inventoryRestClient.get()
        .uri("/inventory")
        .retrieve()
        .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {}));
  }

  @PostMapping("/inventory")
  public ResponseEntity<?> createInventory(@RequestBody Map<String, Object> body) {
    return ResponseEntity.ok(inventoryRestClient.post()
        .uri("/inventory")
        .body(body)
        .retrieve()
        .body(Map.class));
  }

  @PutMapping("/inventory/{sku}")
  public ResponseEntity<?> updateInventory(@PathVariable String sku, @RequestBody Map<String, Object> body) {
    return ResponseEntity.ok(inventoryRestClient.put()
        .uri("/inventory/{sku}", sku)
        .body(body)
        .retrieve()
        .body(Map.class));
  }

  @DeleteMapping("/inventory/{sku}")
  public ResponseEntity<?> deleteInventory(@PathVariable String sku) {
    inventoryRestClient.delete()
        .uri("/inventory/{sku}", sku)
        .retrieve()
        .toBodilessEntity();
    return ResponseEntity.ok().build();
  }

  // Fulfillment APIs
  @GetMapping("/fulfillments")
  public ResponseEntity<List<FulfillmentView>> getAllFulfillments() {
    return ResponseEntity.ok(fulfillmentRestClient.get()
        .uri("/fulfillments")
        .retrieve()
        .body(new ParameterizedTypeReference<List<FulfillmentView>>() {}));
  }

  @PutMapping("/fulfillments/{id}/ship")
  public ResponseEntity<FulfillmentView> shipFulfillment(@PathVariable Long id) {
    return ResponseEntity.ok(fulfillmentRestClient.put()
        .uri("/fulfillments/{id}/ship", id)
        .retrieve()
        .body(FulfillmentView.class));
  }

  // Analytics APIs
  @GetMapping("/analytics/summary")
  public ResponseEntity<?> getAnalyticsSummary(@RequestParam String date) {
    return ResponseEntity.ok(analyticsRestClient.get()
        .uri("/api/analytics/orders/summary?date={date}", date)
        .retrieve()
        .body(Map.class));
  }

  @GetMapping("/analytics/events/count")
  public ResponseEntity<?> getEventCount() {
    return ResponseEntity.ok(analyticsRestClient.get()
        .uri("/api/analytics/events/count")
        .retrieve()
        .body(Map.class));
  }
}

// Made with Bob
