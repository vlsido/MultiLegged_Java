package com.coslavko.multilegged.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.coslavko.multilegged.dto.ContactDTO;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@Service
public class EmailService {

  @Autowired
  private JavaMailSender mailSender;

  public void sendContactEmail(ContactDTO contact) throws MessagingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);

    helper.setTo("multileggedstore@gmail.com");
    helper.setSubject("Contact Form Subject: " + contact.getSubject());
    helper.setText(
        "Name: " + contact.getName() + "\n" +
            "Email: " + contact.getEmail() + "\n\n" +
            contact.getMessage());

    mailSender.send(message);
  }
}
