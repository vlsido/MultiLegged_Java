package com.coslavko.multilegged.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Species {
  private int id;
  private String imageUrl;
  private List<String> names;
  private String description;
  private int units;
  private List<SpeciesPack> speciesPacks = new ArrayList<>();

}
