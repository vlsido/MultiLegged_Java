package com.coslavko.multilegged.model;

import lombok.Data;

@Data
public class AnimalPrice {
  private int id;
  private int min_quantity;
  private Integer max_quantity;
  private int cents_per_unit;
}
