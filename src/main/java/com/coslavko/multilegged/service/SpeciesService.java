package com.coslavko.multilegged.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.coslavko.multilegged.model.Category;
import com.coslavko.multilegged.model.Species;

@Service
public class SpeciesService {
  private final JdbcTemplate jdbcTemplate;

  public SpeciesService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Category> fetchSpecies() {
    String sql = """
                SELECT
                       c.id AS category_id,
                       c.name AS category_name,
                       s.id AS species_id,
                       s.names,
                       s.description,
                       s.units AS species_units,
                       sp.units_per_pack,
                       sp.price_cents
                FROM categories c
                JOIN species s ON s.category_id = c.id
                LEFT JOIN species_packs sp ON sp.species_id = s.id
                ORDER BY c.name, s.id;
        """;

    return jdbcTemplate.query(sql, rs -> {
      Map<String, Category> categoryMap = new LinkedHashMap<>();
      Map<Integer, Species> speciesMap = new HashMap<>();

      while (rs.next()) {
        String categoryName = rs.getString("category_name");
        Integer categoryId = rs.getInt("category_id");
        Integer speciesId = rs.getInt("species_id");

        Category category = categoryMap.computeIfAbsent(categoryName, g -> {
          Category aCategory = new Category();

          aCategory.setId(categoryId);
          aCategory.setName(categoryName);

          return aCategory;
        });

      }
    });
  }
}
