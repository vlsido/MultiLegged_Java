package com.coslavko.multilegged.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coslavko.multilegged.model.SpeciesCategory;
import com.coslavko.multilegged.service.SpeciesService;

@RestController
public class SpeciesController {

  private final SpeciesService speciesService;

  public SpeciesController(SpeciesService speciesService) {
    this.speciesService = speciesService;
  }

  @GetMapping("/api/species")
  public List<SpeciesCategory> species() {

    return speciesService.fetchSpecies();

  }
}
