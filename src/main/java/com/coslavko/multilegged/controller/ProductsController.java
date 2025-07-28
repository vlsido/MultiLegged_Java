package com.coslavko.multilegged.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coslavko.multilegged.model.ProductCategory;
import com.coslavko.multilegged.service.ProductsService;

@RestController
public class ProductsController {

  private final ProductsService productsService;

  public ProductsController(ProductsService productsService) {
    this.productsService = productsService;
  }

  @GetMapping("/api/products")
  public List<ProductCategory> products() {

    return productsService.fetchProducts();

  }
}
