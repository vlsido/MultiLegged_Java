package com.coslavko.multilegged.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.coslavko.multilegged.dto.CheckoutDTO;
import com.coslavko.multilegged.model.StoreProduct;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.PriceCollection;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.PriceListParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.ProductUpdateParams;
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

    List<Map<String, Object>> prices = new ArrayList<Map<String, Object>>();

    jdbcTemplate.query(sql, params, rs -> {

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
      return;
    });

    Map<Integer, StoreProduct> storeProductMap = new LinkedHashMap<>();

    for (Map<String, Object> price : prices) {
      Integer animalId = (Integer) price.get("animal_id");
      String name = (String) price.get("name");
      int centsPerUnit = (Integer) price.get("cents_per_unit");
      Integer maxQuantity = (Integer) price.get("max_quantity");

      StoreProduct storeProduct = storeProductMap.get(animalId);

      if (storeProduct == null) {
        storeProduct = new StoreProduct();
        storeProduct.setStoreProductId(animalId);
        storeProduct.setName(name);
        storeProduct.setTiers(new ArrayList<>());
        storeProductMap.put(animalId, storeProduct);
      }

      StoreProduct.Tier tier = new StoreProduct.Tier();
      tier.setCentsPerUnit(centsPerUnit);
      tier.setMaxQuantity(maxQuantity);

      storeProduct.getTiers().add(tier);
    }

    List<StoreProduct> storeProducts = new ArrayList<>(storeProductMap.values());

    for (StoreProduct storeProduct : storeProducts) {
      try {
        Product stripeProduct = createOrUpdateProduct(storeProduct.getStoreProductId(), storeProduct.getName());

        createOrUpdatePrice(stripeProduct, storeProduct);
      } catch (StripeException e) {
        throw e;
      }
    }
  }

  private Product createOrUpdateProduct(Integer storeProductId, String name) throws StripeException {
    try {
      Product product = Product.retrieve("prod_" + storeProductId);

      Product updatedProduct = product.update(
          ProductUpdateParams.builder()
              .setName(name)
              .build());
      return updatedProduct;

    } catch (StripeException e) {
      if (e.getCode().equals("resource_missing")) {
        return Product.create(
            ProductCreateParams.builder()
                .setId("prod_" + storeProductId)
                .setName(name)
                .build());
      }
      throw e;
    } catch (Exception e) {
      System.out.println("Another problem occurred, maybe unrelated to Stripe.");
      throw e;
    }
  }

  private void createOrUpdatePrice(Product stripeProduct, StoreProduct storeProduct) throws StripeException {
    String productId = stripeProduct.getId();
    String defaultPriceId = stripeProduct.getDefaultPrice();

    if (defaultPriceId == null) {
      createTieredPrice(productId, storeProduct);
    }
  }

  private Price findPrice(String productId) throws StripeException {

    PriceListParams params = PriceListParams.builder()
        .setProduct(productId)
        .setActive(true)
        .build();

    PriceCollection prices = Price.list(params);

    for (Price price : prices.getData()) {
      if (price.getType().equals("one_time") &&
          price.getBillingScheme().equals("tiered") &&
          price.getTiersMode().equals("volume")) {
        return price;
      }
    }

    return null;
  }

  private Price createTieredPrice(String productId, StoreProduct storeProduct) throws StripeException {
    try {
      List<PriceCreateParams.Tier> stripeTiers = new ArrayList<>();
      List<StoreProduct.Tier> tiers = storeProduct.getTiers();

      for (int i = 0; i < tiers.size(); i++) {
        StoreProduct.Tier tier = tiers.get(i);
        PriceCreateParams.Tier.Builder tBuilder = PriceCreateParams.Tier.builder()
            .setUnitAmount((long) tier.getCentsPerUnit());

        // If this is the last tier, set up_to to "inf" (infinity)
        if (i == tiers.size() - 1) {
          // For the last tier, we need to explicitly use "inf" string
          tBuilder.putExtraParam("up_to", "inf");
        } else {
          // For all other tiers, we need a numeric up_to
          if (tier.getMaxQuantity() == null) {
            throw new IllegalArgumentException("All tiers except the last must have a maxQuantity");
          }
          tBuilder.setUpTo((long) tier.getMaxQuantity());
        }

        stripeTiers.add(tBuilder.build());
      }

      PriceCreateParams params = PriceCreateParams.builder()
          .setProduct(productId)
          .setCurrency("eur")
          .setTiersMode(PriceCreateParams.TiersMode.VOLUME)
          .addAllTier(stripeTiers)
          .setBillingScheme(PriceCreateParams.BillingScheme.TIERED)
          .setActive(true)
          .build();

      return Price.create(params);
    } catch (Exception e) {
      System.err.println("Failed to create tiered price:");
      System.err.println("Product ID: " + productId);
      System.err.println("Tiers: " + storeProduct.getTiers());
      e.printStackTrace();
      throw e;
    }
  }
  // private Price createTieredPrice(String productId, StoreProduct storeProduct)
  // throws StripeException {
  // try {
  // List<PriceCreateParams.Tier> stripeTiers = new ArrayList<>();
  //
  // for (StoreProduct.Tier tier : storeProduct.getTiers()) {
  // PriceCreateParams.Tier.Builder tBuilder = PriceCreateParams.Tier.builder()
  // .setUnitAmount((long) tier.getCentsPerUnit());
  //
  // if (tier.getMaxQuantity() != null) {
  // tBuilder.setUpTo((long) tier.getMaxQuantity());
  // } else {
  // tBuilder.setUpTo("inf");
  // }
  //
  // stripeTiers.add(tBuilder.build());
  // }
  //
  // PriceCreateParams params = PriceCreateParams.builder()
  // .setProduct(productId)
  // .setCurrency("eur")
  // .setTiersMode(PriceCreateParams.TiersMode.VOLUME)
  // .addAllTier(stripeTiers)
  // .setBillingScheme(PriceCreateParams.BillingScheme.TIERED)
  // .build();
  //
  // return Price.create(params);
  // } catch (Exception e) {
  // System.err.println("Failed to create tiered price:");
  // System.err.println("Product ID: " + productId);
  // System.err.println("Tiers: " + storeProduct.getTiers());
  // e.printStackTrace();
  // throw e;
  // }
  // }

  public Map<String, String> createCheckoutSession(List<CheckoutDTO> checkoutDTOs) throws Exception {
    SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
        .setUiMode(SessionCreateParams.UiMode.CUSTOM)
        .setMode(SessionCreateParams.Mode.PAYMENT)
        .setReturnUrl(domainUrls[0] + "/cart/return?session_id={CHECKOUT_SESSION_ID}");

    for (CheckoutDTO item : checkoutDTOs) {
      Price tieredPrice = findPrice("prod_" + item.getAnimalId());

      if (tieredPrice == null) {
        throw new Exception("Couldn't find price");
      }

      paramsBuilder.addLineItem(
          SessionCreateParams.LineItem.builder()
              .setQuantity((long) item.getQuantity())
              .setPrice(tieredPrice.getId())
              .build());
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
