package com.coslavko.multilegged.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coslavko.multilegged.model.AnimalCategory;
import com.coslavko.multilegged.service.AnimalsService;

@RestController
public class AnimalsController {

  private final AnimalsService animalsService;

  public AnimalsController(AnimalsService animalsService) {
    this.animalsService = animalsService;
  }

  @GetMapping("/api/species")
  public List<AnimalCategory> species() {

    return animalsService.fetchSpecies();

  }
}
