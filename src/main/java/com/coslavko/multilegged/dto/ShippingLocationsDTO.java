package com.coslavko.multilegged.dto;

import java.util.List;

import lombok.Data;

@Data
public class ShippingLocationsDTO {
  private String companyName;
  private List<Location> locations;

  @Data
  public static class Location {
    private String name;
    private String countryCode;
  }
}
