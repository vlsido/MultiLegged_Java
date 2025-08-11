package com.coslavko.multilegged.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank String phone,
    @NotEmpty List<Item> items) {
  public record Item(
      @NotNull int productId,
      @NotNull int quantity) {
  }
}
