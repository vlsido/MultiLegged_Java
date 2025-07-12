package com.coslavko.multilegged.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class CheckoutService {
  public Map<String, String> createCheckoutSession() throws StripeException {
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
