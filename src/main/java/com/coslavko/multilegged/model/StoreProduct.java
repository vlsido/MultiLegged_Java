package com.coslavko.multilegged.model;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StoreProduct {
  private int storeProductId;
  private String name;
  private List<Tier> tiers;

  @Data
  @NoArgsConstructor
  public static class Tier {
    private int centsPerUnit;
    private Integer maxQuantity;
  }
}
