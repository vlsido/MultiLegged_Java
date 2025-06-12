package com.coslavko.multilegged.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Species {
  private int id;
  private String imageUrl;
  private List<String> names;
  private String description;
  private int units;
  private List<SpeciesPack> speciesPacks;

  public Species(
      @JsonProperty("id") int id,
      @JsonProperty("imageUrl") String imageUrl,
      @JsonProperty("names") List<String> names,
      @JsonProperty("description") String description,
      @JsonProperty("units") int units,
      @JsonProperty("speciesPacks") List<SpeciesPack> speciesPacks) {
    this.id = id;
    this.imageUrl = imageUrl;
    this.names = names;
    this.description = description;
    this.units = units;
    this.speciesPacks = speciesPacks;
  }

  public int getId() {
    return id;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public List<String> getNames() {
    return names;
  }

  public String getDescription() {
    return description;
  }

  public int getUnits() {
    return units;
  }

}
