package com.coslavko.multilegged.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CheckoutProduct {
  private int productId;
  private String name;
  private int quantity;
  private int centsPerUnit;
}
