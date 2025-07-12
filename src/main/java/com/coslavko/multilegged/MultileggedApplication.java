package com.coslavko.multilegged;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.stripe.Stripe;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class MultileggedApplication {

  @Value("${app.stripe.apikey}")
  private String stripeApiKey;

  @PostConstruct
  public void init() {
    Stripe.apiKey = stripeApiKey;
  }

  public static void main(String[] args) {
    SpringApplication.run(MultileggedApplication.class, args);
  }

}
