package com.coslavko.multilegged.model;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class CheckoutProduct {
  private int productId;
  private String name;
  private int quantity;
  private int centsPerUnit;
}
