package com.coslavko.multilegged.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Product {
  private int id;
  private String imageUrl;
  private String name;
  private String origin;
  private String size;
  private String humidity;
  private String temperature;
  private String description;
  private int units;
  private List<ProductPrice> productPrices = new ArrayList<>();
  private String form;

}
