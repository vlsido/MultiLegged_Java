package com.coslavko.multilegged.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Animal {
  private int id;
  private String imageUrl;
  private String name;
  private String description;
  private int units;
  private List<AnimalPrice> animalPrices = new ArrayList<>();
  private String form;

}
