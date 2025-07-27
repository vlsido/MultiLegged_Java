package com.coslavko.multilegged.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coslavko.multilegged.dto.ShippingLocationsDTO;
import com.coslavko.multilegged.service.ShippingService;

@RestController
public class ShippingController {

  private final ShippingService shippingService;

  public ShippingController(ShippingService shippingService) {
    this.shippingService = shippingService;
  }

  @GetMapping("/api/shipping-locations")
  public ResponseEntity<List<ShippingLocationsDTO>> shippingLocations() {
    List<ShippingLocationsDTO> shippingLocationsDTOs = shippingService.getShippingLocations();
    return ResponseEntity.ok(shippingLocationsDTOs);
  }
}
