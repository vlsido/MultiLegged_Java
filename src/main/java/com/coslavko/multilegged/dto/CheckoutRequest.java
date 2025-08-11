package com.coslavko.multilegged.dto;

import java.util.List;

public record CheckoutRequest(
    String firstName,
    String lastName,
    String phone,
    List<Item> items) {
  public record Item(
      int productId,
      int quantity) {
  }
}
