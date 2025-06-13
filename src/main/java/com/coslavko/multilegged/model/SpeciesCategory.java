package com.coslavko.multilegged.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SpeciesCategory {
  private String name;
  private List<Species> data = new ArrayList<>();

}
