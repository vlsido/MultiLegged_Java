
package com.coslavko.multilegged.dto;

import java.util.List;

import lombok.Data;

@Data
public class CheckoutDTO {
  private String firstName;
  private String lastName;
  private String phone;
  private List<Item> items;

  @Data
  public static class Item {
    private int productId;
    private int quantity;
  }
}
