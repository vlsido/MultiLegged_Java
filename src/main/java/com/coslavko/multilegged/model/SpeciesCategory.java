package com.coslavko.multilegged.model;

import java.util.List;

public class SpeciesCategory {
  private String name;
  private List<Species> species;

  // public Category(
  // @JsonProperty("id") Integer id,
  // @JsonProperty("name") String name) {
  // this.id = id;
  // this.name = name;
  // }

  public String getName() {
    return name;
  }

  public List<Species> getSpecies() {
    return species;
  }

  public void setName(String categoryName) {
    name = categoryName;
  }
}
