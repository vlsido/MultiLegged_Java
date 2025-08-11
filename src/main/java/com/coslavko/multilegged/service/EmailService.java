package com.coslavko.multilegged.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.coslavko.multilegged.dto.ContactRequest;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@Service
public class EmailService {

  @Autowired
  private JavaMailSender mailSender;

  public void sendContactEmail(ContactRequest req) throws MessagingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);

    helper.setTo("multileggedstore@gmail.com");
    helper.setSubject("Contact Form Subject: " + req.getSubject());
    helper.setText(
        "Name: " + req.getName() + "\n" +
            "Email: " + req.getEmail() + "\n\n" +
            req.getMessage());

    mailSender.send(message);
  }
}
