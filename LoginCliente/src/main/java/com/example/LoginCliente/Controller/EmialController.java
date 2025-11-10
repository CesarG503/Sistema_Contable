package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmialController {
    @Autowired
    EmailService emailService;

    @GetMapping("/email/send")
    public ResponseEntity<?> sendEmail() {
        emailService.sendEmail("js3729395@gmail.com", "System OneDi", "Prueba de envio de correo");
    return new ResponseEntity("Correo enviado con exito", HttpStatus.OK);
    }
}
