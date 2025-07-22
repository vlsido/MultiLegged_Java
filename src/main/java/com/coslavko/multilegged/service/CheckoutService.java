package com.coslavko.multilegged.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.coslavko.multilegged.dto.CheckoutDTO;
import com.coslavko.multilegged.model.CheckoutProduct;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class CheckoutService {
  @Autowired
  private NamedParameterJdbcTemplate jdbcTemplate;

  @Value("${app.domain.urls}")
  private String[] domainUrls;

  public CheckoutService(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<CheckoutProduct> getProducts(List<CheckoutDTO> checkoutDTOs) throws Exception {
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

    List<Map<String, Object>> prices = jdbcTemplate.query(sql, params, (rs, rowNum) -> {
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
      price.put("min_quantity", minQuantity);
      price.put("max_quantity", maxQuantity);

      return price;
    });

    Map<Integer, CheckoutProduct> checkoutProductMap = new LinkedHashMap<>();

    for (Map<String, Object> price : prices) {
      Integer animalId = (Integer) price.get("animal_id");
      String name = (String) price.get("name");
      int centsPerUnit = (Integer) price.get("cents_per_unit");
      int minQuantity = (Integer) price.get("min_quantity");

      CheckoutProduct checkoutProduct = checkoutProductMap.get(animalId);

      if (checkoutProduct == null) {
        Optional<CheckoutDTO> matchingProduct = checkoutDTOs.stream()
            .filter(item -> item.getAnimalId() == animalId)
            .findFirst();

        if (matchingProduct.isEmpty()) {
          throw new Exception("AnimalId is not found in checkoutDTOs: " + animalId);
        }

        int quantity = matchingProduct.get().getQuantity();

        checkoutProduct = new CheckoutProduct();
        checkoutProduct.setProductId(animalId);
        checkoutProduct.setName(name);
        checkoutProduct.setQuantity(quantity);
        checkoutProductMap.put(animalId, checkoutProduct);
      }

      if (checkoutProduct.getQuantity() >= minQuantity) {
        checkoutProduct.setCentsPerUnit(centsPerUnit);
      }

    }

    List<CheckoutProduct> checkoutProducts = new ArrayList<>(checkoutProductMap.values());

    return checkoutProducts;
  }

  public Map<String, String> createCheckoutSession(List<CheckoutDTO> checkoutDTOs) throws Exception {
    SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
        .setUiMode(SessionCreateParams.UiMode.CUSTOM)
        .setMode(SessionCreateParams.Mode.PAYMENT)
        .setReturnUrl(domainUrls[0] + "/cart/return?session_id={CHECKOUT_SESSION_ID}");

    List<CheckoutProduct> checkoutProducts = getProducts(checkoutDTOs);

    for (CheckoutProduct item : checkoutProducts) {
      SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
          .setQuantity((long) item.getQuantity())
          .setPriceData(
              SessionCreateParams.LineItem.PriceData.builder()
                  .setCurrency("eur")
                  .setUnitAmount((long) item.getCentsPerUnit())
                  .setProductData(
                      SessionCreateParams.LineItem.PriceData.ProductData.builder()
                          .setName(item.getName())
                          .build())
                  .build())
          .build();

      paramsBuilder.addLineItem(lineItem);
    }

    Session session = Session.create(paramsBuilder.build());

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
