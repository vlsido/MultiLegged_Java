package com.coslavko.multilegged.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.coslavko.multilegged.dto.ShippingLocationsResponse;
import com.coslavko.multilegged.dto.ShippingLocationsResponse.Location;

@Service
public class ShippingService {

  private final RestTemplate restTemplate;

  public ShippingService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public List<ShippingLocationsResponse> getShippingLocations() {
    List<ShippingLocationsResponse> result = new ArrayList<>();

    List<Map<String, Object>> dpdRaw = fetchData("https://dpdbaltics.com/PickupParcelShopData.json");
    result.add(parseDPD(dpdRaw));

    List<Map<String, Object>> omnivaRaw = fetchData("https://www.omniva.ee/locations.json");
    result.add(parseOmniva(omnivaRaw));

    return result;
  }

  private ShippingLocationsResponse parseDPD(List<Map<String, Object>> raw) {
    List<Location> locations = raw.stream().map(item -> new ShippingLocationsResponse.Location(
        (String) item.get("companyName"),
        (String) item.get("countryCode"))).sorted(Comparator.comparing(location -> {
          String[] parts = location.name().split("\\s+");
          return parts.length > 1 ? parts[1] : "";
        })).toList();

    return new ShippingLocationsResponse("DPD", locations);
  }

  private ShippingLocationsResponse parseOmniva(List<Map<String, Object>> raw) {
    List<ShippingLocationsResponse.Location> locations = raw.stream()
        .map(item -> new ShippingLocationsResponse.Location(
            (String) item.get("NAME"),
            (String) item.get("A0_NAME")))
        .toList();

    return new ShippingLocationsResponse("Omniva", locations);
  }

  private List<Map<String, Object>> fetchData(String url) {
    return restTemplate
        .exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {
        }).getBody();
  }
}
