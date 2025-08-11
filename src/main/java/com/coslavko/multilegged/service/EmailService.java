package com.coslavko.multilegged.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.coslavko.multilegged.dto.ContactRequest;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

  @Autowired
  private JavaMailSender mailSender;

  public void sendContactEmail(ContactRequest req) throws MessagingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);

    helper.setTo("multileggedstore@gmail.com");
    helper.setSubject("Contact Form Subject: " + req.subject());
    helper.setText(
        "Name: " + req.name() + "\n" +
            "Email: " + req.email() + "\n\n" +
            req.message());

    mailSender.send(message);
  }
}
