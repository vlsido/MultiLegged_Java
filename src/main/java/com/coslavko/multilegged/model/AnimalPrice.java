package com.coslavko.multilegged.model;

import lombok.Data;

@Data
public class AnimalPrice {
  private int id;
  private int minQuantity;
  private Integer maxQuantity;
  private int centsPerUnit;
}
