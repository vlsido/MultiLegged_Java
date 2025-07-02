package com.coslavko.multilegged.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AnimalCategory {
  private String category;
  private List<Animal> animals = new ArrayList<>();

}
