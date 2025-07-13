package com.coslavko.multilegged.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.coslavko.multilegged.dto.CheckoutDTO;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.ProductUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class CheckoutService {
  @Autowired
  private NamedParameterJdbcTemplate jdbcTemplate;

  public CheckoutService(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void syncStripeProducts(List<CheckoutDTO> checkoutDTOs) throws Exception {
    String sql = """
                   SELECT
                           a.name,
                           a.id as animal_id,
                           ap.id as price_id,
                           ap.cents_per_unit,
                           ap.min_quantity,
                           ap.max_quantity,
                           a.units,
                           a.form
                    FROM animal_prices ap
                    JOIN animals a ON a.id = ap.animal_id
                    WHERE a.id IN (:ids)
                    ORDER BY price_id;
        """;

    List<Integer> animalIds = checkoutDTOs.stream()
        .map(CheckoutDTO::getAnimalId)
        .distinct()
        .toList();

    Map<String, Object> params = Map.of("ids", animalIds);

    jdbcTemplate.query(sql, params, rs -> {
      List<Map<String, Object>> prices = new ArrayList<Map<String, Object>>();

      while (rs.next()) {
        Map<String, Object> price = new LinkedHashMap<>();

        int animalId = rs.getInt("animal_id");
        String name = rs.getString("name") + "(" + rs.getString("form") + ")";
        int centsPerUnit = rs.getInt("cents_per_unit");

        int minQuantity = rs.getInt("min_quantity");

        Integer maxQuantity = null;
        int maxQuantityValue = rs.getInt("max_quantity");
        if (!rs.wasNull()) {
          maxQuantity = maxQuantityValue;
        }

        price.put("animal_id", animalId);
        price.put("name", name);
        price.put("cents_per_unit", centsPerUnit);
        price.put("max_quantity", maxQuantity);

        prices.add(price);
      }
      // try {
      // // Product product = createOrUpdateProduct(animalId, name);
      //
      // // createOrUpdatePrice(product.get(), prices)
      // } catch (StripeException e) {
      // return;
      // }
      return;
    });
  }

  // private Price createOrUpdatePrice(String productId, List<Map<String, Object>>
  // prices) throws StripeException {
  //
  // List<PriceCreateParams.Tier> tiers = new ArrayList<>();
  //
  // }

  private Product createOrUpdateProduct(Integer animalId, String name) throws StripeException {
    try {
      Product product = Product.retrieve("prod_" + animalId);

      Product updatedProduct = product.update(
          ProductUpdateParams.builder()
              .setName(name)
              .build());
      return updatedProduct;

    } catch (StripeException e) {
      if (e.getCode().equals("resource_missing")) {
        return Product.create(
            ProductCreateParams.builder()
                .setId("prod_" + animalId)
                .setName(name)
                .build());
      }
      throw e;
    } catch (Exception e) {
      System.out.println("Another problem occurred, maybe unrelated to Stripe.");
      throw e;
    }
  }

  public Map<String, String> createCheckoutSession(List<CheckoutDTO> checkoutDTOs) throws StripeException {
    String DOMAIN = "http://192.168.0.102:5173";
    SessionCreateParams params = SessionCreateParams.builder()
        .setUiMode(SessionCreateParams.UiMode.CUSTOM)
        .setMode(SessionCreateParams.Mode.PAYMENT)
        .setReturnUrl(DOMAIN + "/cart/return?session_id={CHECKOUT_SESSION_ID}")
        .addLineItem(
            SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPrice("price_1RjhjqPI2Lgb9onPgvSbUiBk")
                .build())
        .build();

    Session session = Session.create(params);

    Map<String, String> map = new HashMap<>();

    map.put("clientSecret", session.getRawJsonObject().getAsJsonPrimitive("client_secret").getAsString());

    return map;
  }

  public Map<String, String> getSessionStatus(String session_id) throws StripeException {
    Session session = Session.retrieve(session_id);

    Map<String, String> map = new HashMap<>();

    map.put("status", session.getRawJsonObject().getAsJsonPrimitive("status").getAsString());
    map.put("customer_email",
        session.getRawJsonObject().getAsJsonObject("customer_details").getAsJsonPrimitive("email").getAsString());

    return map;
  }
}
