package com.example.LoginCliente.Security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String code = "bad"; // genérico

        if (exception instanceof UsernameNotFoundException) {
            code = "nouser";
        } else if (exception instanceof BadCredentialsException) {
            code = "bad"; // usuario existe pero contraseña errónea
        }

        response.sendRedirect(request.getContextPath()
                + "/usuarios/auth?panel=login&loginErr=" + code);
    }
}

