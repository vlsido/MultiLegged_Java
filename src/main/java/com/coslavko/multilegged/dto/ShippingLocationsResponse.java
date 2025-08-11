package com.coslavko.multilegged.dto;

import java.util.List;

public record ShippingLocationsResponse(
    String companyName,
    List<Location> locations) {
  public record Location(
      String name,
      String countryCode) {
  }
}
