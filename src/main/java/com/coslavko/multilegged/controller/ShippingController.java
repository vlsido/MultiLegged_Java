package com.coslavko.multilegged.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coslavko.multilegged.dto.ShippingLocationsResponse;
import com.coslavko.multilegged.service.ShippingService;

@RestController
@RequestMapping("/v1/locations")
public class ShippingController {

  private final ShippingService shippingService;

  public ShippingController(ShippingService shippingService) {
    this.shippingService = shippingService;
  }

  @GetMapping
  public ResponseEntity<List<ShippingLocationsResponse>> shippingLocations() {
    List<ShippingLocationsResponse> responses = shippingService.getShippingLocations();
    return ResponseEntity.ok(responses);
  }
}
