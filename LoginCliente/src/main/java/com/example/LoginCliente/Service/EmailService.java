package com.example.LoginCliente.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    JavaMailSender mailSender;
    @Autowired
    private JavaMailSender javaMailSender;

    public void sendEmail(String to, String subject, String text) {
        // Implementation for sending email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("juansp.ues@gmail.com");
        message.setTo("juansp.ues@gmail.com");
        message.setSubject("Prueba envió email simple");
        message.setText("Esto es el contenido del email");

        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        javaMailSender.send(message);
    }
}
