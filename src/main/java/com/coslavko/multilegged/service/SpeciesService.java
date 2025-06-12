package com.coslavko.multilegged.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.coslavko.multilegged.model.SpeciesCategory;
import com.coslavko.multilegged.model.SpeciesPack;
import com.coslavko.multilegged.model.Species;

@Service
public class SpeciesService {
  private final JdbcTemplate jdbcTemplate;

  public SpeciesService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<SpeciesCategory> fetchSpecies() {
    String sql = """
                SELECT
                       c.name AS category_name,
                       s.id AS species_id,
                       s.names,
                       s.image_url,
                       s.description,
                       s.units AS species_units,
                       sp.id as pack_id,
                       sp.units_per_pack,
                       sp.price_cents
                FROM categories c
                JOIN species s ON s.category_id = c.id
                LEFT JOIN species_packs sp ON sp.species_id = s.id
                ORDER BY c.name, s.id;
        """;

    return jdbcTemplate.query(sql, rs -> {
      Map<String, SpeciesCategory> categoryMap = new LinkedHashMap<>();
      Map<Integer, Species> speciesMap = new HashMap<>();

      while (rs.next()) {
        String categoryName = rs.getString("category_name");
        Integer speciesId = rs.getInt("species_id");

        SpeciesCategory category = categoryMap.computeIfAbsent(categoryName, c -> {
          SpeciesCategory aCategory = new SpeciesCategory();

          aCategory.setName(c);

          return aCategory;
        });

        Species species = speciesMap.computeIfAbsent(speciesId, id -> {
          try {
            List<String> names = Arrays.asList(rs.getString("names").split(","));
            Species s = new Species();
            s.setId(id);
            s.setNames(names);
            s.setImageUrl(rs.getString("image_url"));
            s.setDescription(rs.getString("description"));
            s.setUnits(rs.getInt("species_units"));
            category.getSpecies().add(s);

            return s;
          } catch (SQLException e) {
            throw new RuntimeException("Failed to map species from result set", e);
          }
        });

        int unitsPerPack = rs.getInt("units_per_pack");
        if (!rs.wasNull()) {
          SpeciesPack pack = new SpeciesPack();
          pack.setId(rs.getInt("pack_id"));
          pack.setUnits(unitsPerPack);
          pack.setPrice(rs.getInt("price_cents"));
          species.getSpeciesPacks().add(pack);
        }
      }

      return new ArrayList<>(categoryMap.values());
    });
  }
}
