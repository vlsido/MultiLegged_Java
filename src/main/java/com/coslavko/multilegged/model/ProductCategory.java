package com.coslavko.multilegged.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ProductCategory {
  private String category;
  private List<Product> products = new ArrayList<>();
}
