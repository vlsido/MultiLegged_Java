package com.coslavko.multilegged.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.coslavko.multilegged.dto.ContactDTO;
import com.coslavko.multilegged.service.EmailService;

@RestController
public class ContactController {

  private final EmailService emailService;

  public ContactController(EmailService emailService) {
    this.emailService = emailService;
  }

  @PostMapping("/api/contact")
  public ResponseEntity<String> contact(@RequestBody ContactDTO contact) {
    try {
      emailService.sendContactEmail(contact);
      return ResponseEntity.ok("Message sent");
    } catch (Exception e) {
      return ResponseEntity.status(500).body("Failed to send message");
    }

  }
}
