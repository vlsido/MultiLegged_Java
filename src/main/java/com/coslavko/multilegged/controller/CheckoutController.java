package com.coslavko.multilegged.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coslavko.multilegged.dto.CheckoutDTO;
import com.coslavko.multilegged.service.CheckoutService;

@RestController
public class CheckoutController {
  private final CheckoutService checkoutService;

  public CheckoutController(CheckoutService checkoutService) {
    this.checkoutService = checkoutService;
  }

  @PostMapping("/api/create-checkout-session")
  public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody CheckoutDTO checkoutDTO) {
    Map<String, String> response = new HashMap<>();
    try {
      response = checkoutService.createCheckoutSession(checkoutDTO);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      response.put("error", "Failed to create checkout session");
      return ResponseEntity.status(500).body(response);
    }
  }

  @GetMapping("/api/session-status")
  public ResponseEntity<Map<String, String>> sessionStatus(@RequestParam("session_id") String session_id) {
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
