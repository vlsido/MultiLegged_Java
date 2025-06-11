package com.coslavko.multilegged.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Categories {
  private Integer id;
  private String name;

  public Categories(
      @JsonProperty("id") Integer id,
      @JsonProperty("name") String name) {
    this.id = id;
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
