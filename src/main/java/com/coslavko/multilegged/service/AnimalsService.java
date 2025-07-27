package com.coslavko.multilegged.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.coslavko.multilegged.model.AnimalCategory;
import com.coslavko.multilegged.model.AnimalPrice;
import com.coslavko.multilegged.model.Animal;

@Service
public class AnimalsService {
  private final JdbcTemplate jdbcTemplate;

  public AnimalsService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<AnimalCategory> fetchAnimals() {
    String sql = """
                SELECT
                       c.name AS category_name,
                       a.id AS animal_id,
                       a.name,
                       a.image_url,
                       a.origin,
                       a.size,
                       a.humidity,
                       a.temperature,
                       a.description,
                       a.units AS animal_units,
                       a.form,
                       ap.id as price_id,
                       ap.cents_per_unit,
                       ap.min_quantity,
                       ap.max_quantity
                FROM categories c
                JOIN animals a ON a.category_id = c.id
                LEFT JOIN animal_prices ap ON ap.animal_id = a.id
                ORDER BY c.name, a.id;
        """;

    return jdbcTemplate.query(sql, rs -> {
      Map<String, AnimalCategory> categoryMap = new LinkedHashMap<>();
      Map<Integer, Animal> animalMap = new HashMap<>();

      while (rs.next()) {
        String categoryName = rs.getString("category_name");
        Integer speciesId = rs.getInt("animal_id");

        AnimalCategory category = categoryMap.computeIfAbsent(categoryName, c -> {
          AnimalCategory aCategory = new AnimalCategory();

          aCategory.setCategory(c);

          return aCategory;
        });

        Animal animal = animalMap.computeIfAbsent(speciesId, id -> {
          try {
            Animal a = new Animal();
            a.setId(id);
            a.setName(rs.getString("name"));
            a.setImageUrl(rs.getString("image_url"));
            a.setOrigin(rs.getString("origin"));
            a.setSize(rs.getString("size"));
            a.setHumidity(rs.getString("humidity"));
            a.setTemperature(rs.getString("temperature"));
            a.setDescription(rs.getString("description"));
            a.setForm(rs.getString("form"));
            a.setUnits(rs.getInt("animal_units"));
            category.getAnimals().add(a);

            return a;
          } catch (SQLException e) {
            throw new RuntimeException("Failed to map species from result set", e);
          }
        });

        if (!rs.wasNull()) {
          int minQuantity = rs.getInt("min_quantity");
          Integer maxQuantity = null;
          int maxQuantityValue = rs.getInt("max_quantity");
          if (!rs.wasNull()) {
            maxQuantity = maxQuantityValue;
          }

          int centsPerUnit = rs.getInt("cents_per_unit");

          AnimalPrice price = new AnimalPrice();
          price.setId(rs.getInt("price_id"));
          price.setMinQuantity(minQuantity);
          price.setMaxQuantity(maxQuantity);
          price.setCentsPerUnit(centsPerUnit);
          animal.getAnimalPrices().add(price);
        }
      }

      return new ArrayList<>(categoryMap.values());
    });
  }
}
