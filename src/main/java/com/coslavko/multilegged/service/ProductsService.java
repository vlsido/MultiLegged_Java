package com.coslavko.multilegged.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.coslavko.multilegged.model.Product;
import com.coslavko.multilegged.model.ProductCategory;
import com.coslavko.multilegged.model.ProductPrice;

@Service
public class ProductsService {
  private final JdbcTemplate jdbcTemplate;

  public ProductsService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<ProductCategory> fetchProducts() {
    String sql = """
                SELECT
                       c.name AS category_name,
                       a.id AS product_id,
                       a.name,
                       a.image_url,
                       a.origin,
                       a.size,
                       a.humidity,
                       a.temperature,
                       a.description,
                       a.units AS product_units,
                       a.form,
                       ap.id as price_id,
                       ap.cents_per_unit,
                       ap.min_quantity,
                       ap.max_quantity
                FROM categories c
                JOIN products a ON a.category_id = c.id
                LEFT JOIN product_prices ap ON ap.product_id = a.id
                ORDER BY c.name, a.id;
        """;

    return jdbcTemplate.query(sql, rs -> {
      Map<String, ProductCategory> categoryMap = new LinkedHashMap<>();
      Map<Integer, Product> productMap = new HashMap<>();

      while (rs.next()) {
        String categoryName = rs.getString("category_name");
        Integer speciesId = rs.getInt("product_id");

        ProductCategory category = categoryMap.computeIfAbsent(categoryName, c -> {
          ProductCategory aCategory = new ProductCategory();

          aCategory.setCategory(c);

          return aCategory;
        });

        Product product = productMap.computeIfAbsent(speciesId, id -> {
          try {
            Product a = new Product();
            a.setId(id);
            a.setName(rs.getString("name"));
            a.setImageUrl(rs.getString("image_url"));
            a.setOrigin(rs.getString("origin"));
            a.setSize(rs.getString("size"));
            a.setHumidity(rs.getString("humidity"));
            a.setTemperature(rs.getString("temperature"));
            a.setDescription(rs.getString("description"));
            a.setForm(rs.getString("form"));
            a.setUnits(rs.getInt("product_units"));
            category.getProducts().add(a);

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

          ProductPrice price = new ProductPrice();
          price.setId(rs.getInt("price_id"));
          price.setMinQuantity(minQuantity);
          price.setMaxQuantity(maxQuantity);
          price.setCentsPerUnit(centsPerUnit);
          product.getProductPrices().add(price);
        }
      }

      return new ArrayList<>(categoryMap.values());
    });
  }
}
