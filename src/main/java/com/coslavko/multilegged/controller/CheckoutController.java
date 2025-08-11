package com.coslavko.multilegged.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coslavko.multilegged.dto.CheckoutRequest;
import com.coslavko.multilegged.service.CheckoutService;

@RestController
@RequestMapping("/v1/checkout")
public class CheckoutController {
  private final CheckoutService checkoutService;

  public CheckoutController(CheckoutService checkoutService) {
    this.checkoutService = checkoutService;
  }

  @PostMapping
  public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody CheckoutRequest req) {
    Map<String, String> response = new HashMap<>();
    try {
      response = checkoutService.createCheckoutSession(req);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      response.put("error", "Failed to create checkout session");
      return ResponseEntity.status(500).body(response);
    }
  }

  @GetMapping("/session-status")
  public ResponseEntity<Map<String, String>> sessionStatus(@RequestParam String session_id) {
    Map<String, String> response = new HashMap<>();
    try {
      response = checkoutService.getSessionStatus(session_id);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      response.put("error", "Failed to get checkout session status");
      return ResponseEntity.status(500).body(response);
    }
  }
}
