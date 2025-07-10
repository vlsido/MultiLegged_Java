package com.coslavko.multilegged.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ContactDTO {
  private String name;
  private String email;
  private String subject;
  private String message;
}
