package com.coslavko.multilegged.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import com.coslavko.multilegged.dto.CheckoutDTO;
import com.coslavko.multilegged.dto.CheckoutDTO.Item;
import com.coslavko.multilegged.model.CheckoutProduct;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.transaction.Transactional;

@Service
public class CheckoutService {
  @Autowired
  private NamedParameterJdbcTemplate jdbcTemplate;

  @Value("${app.domain.urls}")
  private String[] domainUrls;

  public CheckoutService(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional
  private void addOrder(CheckoutDTO checkoutDTO) throws Exception {
    String sql = """
        INSERT INTO orders (first_name, last_name, phone, status)
        VALUES (:firstName, :lastName, :phone, 'PENDING')
        """;

    Map<String, Object> paramsMap = new HashMap<>();

    paramsMap.put("firstName", checkoutDTO.getFirstName());
    paramsMap.put("lastName", checkoutDTO.getLastName());
    paramsMap.put("phone", checkoutDTO.getPhone());

    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(sql, new MapSqlParameterSource(paramsMap), keyHolder, new String[] { "id" });

    Number generatedId = keyHolder.getKey();
    if (generatedId == null) {
      throw new Exception("Failed to create order");
    }

    int orderId = generatedId.intValue();

    for (Item item : checkoutDTO.getItems()) {
      String ordersProductsSql = """
          INSERT INTO orders_products (order_id, product_id, quantity)
          VALUES (:orderId, :productId, :quantity)
          """;

      Map<String, Object> ordersProductsParamsMap = new HashMap<>();

      ordersProductsParamsMap.put("orderId", orderId);
      ordersProductsParamsMap.put("productId", item.getProductId());
      ordersProductsParamsMap.put("quantity", item.getQuantity());

      jdbcTemplate.update(ordersProductsSql, new MapSqlParameterSource(ordersProductsParamsMap));
    }
  }

  private boolean areCheckoutProductsAvailable(CheckoutDTO checkoutDTO) throws Exception {
    String sql = """
        SELECT
          p.id
          p.units - COALESCE(SUM(op.quantity), 0) AS available_units
          FROM products p
          LEFT JOIN orders_products op ON p.id = op.product_id
          LEFT JOIN orders o ON op.order_id = o.id
          AND o.status = 'PENDING'
          AND o.created_at > NOW() - INTERVAL '15 minutes'
          GROUP BY p.id
        """;

    List<Map<String, Integer>> products = jdbcTemplate.query(sql, (rs, rowNum) -> {
      Map<String, Integer> product = new LinkedHashMap<>();

      int productId = rs.getInt("id");
      int availableUnits = rs.getInt("available_units");

      product.put("productId", productId);
      product.put("availableUnits", availableUnits);

      return product;
    });

    Map<Integer, Integer> productsAvailability = new HashMap<>();

    for (Map<String, Integer> product : products) {
      productsAvailability.put(product.get("productId"), product.get("availableUnits"));
    }

    for (Item item : checkoutDTO.getItems()) {
      int productId = item.getProductId();
      int quantity = item.getQuantity();

      Integer availableUnits = productsAvailability.get(productId);

      if (availableUnits == null) {
        throw new IllegalArgumentException("Product ID " + productId + " not found.");
      }

      if (availableUnits < quantity) {
        throw new IllegalStateException("Not enough units for product ID " + productId);
      }
    }

    return true;
  }

  private List<CheckoutProduct> getProducts(CheckoutDTO checkoutDTO) throws Exception {
    String sql = """
        SELECT
          a.name,
          a.id as product_id,
          ap.id as price_id,
          ap.cents_per_unit,
          ap.min_quantity,
          ap.max_quantity,
          a.units,
          a.form
          FROM product_prices ap
          JOIN products a ON a.id = ap.product_id
          WHERE a.id IN (:ids)
          ORDER BY price_id;
        """;

    List<Integer> productIds = checkoutDTO.getItems().stream()
        .map(CheckoutDTO.Item::getProductId)
        .distinct()
        .toList();

    Map<String, Object> params = Map.of("ids", productIds);

    List<Map<String, Object>> prices = jdbcTemplate.query(sql, params, (rs, rowNum) -> {
      Map<String, Object> price = new LinkedHashMap<>();

      int productId = rs.getInt("product_id");
      String name = rs.getString("name") + "(" + rs.getString("form") + ")";
      int centsPerUnit = rs.getInt("cents_per_unit");

      int minQuantity = rs.getInt("min_quantity");

      Integer maxQuantity = null;
      int maxQuantityValue = rs.getInt("max_quantity");
      if (!rs.wasNull()) {
        maxQuantity = maxQuantityValue;
      }

      price.put("product_id", productId);
      price.put("name", name);
      price.put("cents_per_unit", centsPerUnit);
      price.put("min_quantity", minQuantity);
      price.put("max_quantity", maxQuantity);

      return price;
    });

    Map<Integer, CheckoutProduct> checkoutProductMap = new LinkedHashMap<>();

    for (Map<String, Object> price : prices) {
      Integer productId = (Integer) price.get("product_id");
      String name = (String) price.get("name");
      int centsPerUnit = (Integer) price.get("cents_per_unit");
      int minQuantity = (Integer) price.get("min_quantity");

      CheckoutProduct checkoutProduct = checkoutProductMap.get(productId);

      if (checkoutProduct == null) {
        Optional<CheckoutDTO.Item> matchingProduct = checkoutDTO.getItems().stream()
            .filter(item -> item.getProductId() == productId)
            .findFirst();

        if (matchingProduct.isEmpty()) {
          throw new Exception("ProductId is not found in checkoutDTOs: " + productId);
        }

        int quantity = matchingProduct.get().getQuantity();

        checkoutProduct = new CheckoutProduct();
        checkoutProduct.setProductId(productId);
        checkoutProduct.setName(name);
        checkoutProduct.setQuantity(quantity);
        checkoutProductMap.put(productId, checkoutProduct);
      }

      if (checkoutProduct.getQuantity() >= minQuantity) {
        checkoutProduct.setCentsPerUnit(centsPerUnit);
      }

    }

    List<CheckoutProduct> checkoutProducts = new ArrayList<>(checkoutProductMap.values());

    return checkoutProducts;
  }

  public Map<String, String> createCheckoutSession(CheckoutDTO checkoutDTO) throws Exception {
    SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
        .setUiMode(SessionCreateParams.UiMode.CUSTOM)
        .setMode(SessionCreateParams.Mode.PAYMENT)
        .setReturnUrl(domainUrls[0] + "/cart/return?session_id={CHECKOUT_SESSION_ID}");

    paramsBuilder.addShippingOption(
        SessionCreateParams.ShippingOption.builder()
            .setShippingRateData(
                SessionCreateParams.ShippingOption.ShippingRateData.builder()
                    .setType(SessionCreateParams.ShippingOption.ShippingRateData.Type.FIXED_AMOUNT)
                    .setFixedAmount(
                        SessionCreateParams.ShippingOption.ShippingRateData.FixedAmount.builder()
                            .setAmount(199L)
                            .setCurrency("eur")
                            .build())
                    .setDisplayName("Standard Shipping")
                    .setDeliveryEstimate(
                        SessionCreateParams.ShippingOption.ShippingRateData.DeliveryEstimate.builder()
                            .setMinimum(
                                SessionCreateParams.ShippingOption.ShippingRateData.DeliveryEstimate.Minimum.builder()
                                    .setUnit(
                                        SessionCreateParams.ShippingOption.ShippingRateData.DeliveryEstimate.Minimum.Unit.BUSINESS_DAY)
                                    .setValue(3L)
                                    .build())
                            .setMaximum(
                                SessionCreateParams.ShippingOption.ShippingRateData.DeliveryEstimate.Maximum.builder()
                                    .setUnit(
                                        SessionCreateParams.ShippingOption.ShippingRateData.DeliveryEstimate.Maximum.Unit.BUSINESS_DAY)
                                    .setValue(5L)
                                    .build())
                            .build())
                    .build())
            .build());

    List<CheckoutProduct> checkoutProducts = getProducts(checkoutDTO);

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
