package com.example.LoginCliente.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    TemplateEngine templateEngine;

    @Value("${mail.url}")
    private String URL;

    // El correo que manda el correo :V
    @Value("${spring.mail.username}")
    private String correoFrom;

    public void sendEmail(String to, String subject, String text) {
        // Implementation for sending email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(correoFrom);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        javaMailSender.send(message);
    }

    public void sendEmailHtml(String to, String subject, String nameTemplateHTML) {
        sendEmailHtml(to, subject, nameTemplateHTML, new HashMap<>());
    }

    /**
     * Envía un correo HTML con variables dinámicas
     * @param to Destinatario
     * @param subject Asunto
     * @param nameTemplateHTML Nombre del template (ej: "auth/email-pwd")
     * @param variables Mapa de variables para el template
     */
    public void sendEmailHtml(String to, String subject, String nameTemplateHTML, Map<String, Object> variables) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            Context context = new Context();

            // Agregar URL base
            context.setVariable("url", URL);

            // Agregar todas las variables personalizadas
            variables.forEach(context::setVariable);

            String html = templateEngine.process(nameTemplateHTML, context);
            helper.setFrom(correoFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }
}












