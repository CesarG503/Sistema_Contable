package com.example.LoginCliente.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration  // Indica que esta clase es una configuración de Spring
public class PasswordConfig {

    @Bean  // Define un bean que será administrado por el contenedor de Spring
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // Retorna una instancia de BCryptPasswordEncoder para codificar contraseñas
    }
}