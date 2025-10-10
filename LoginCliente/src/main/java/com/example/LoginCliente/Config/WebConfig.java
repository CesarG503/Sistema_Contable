package com.example.LoginCliente.Config;

import com.example.LoginCliente.Security.UserSessionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private UserSessionInterceptor userSessionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userSessionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/usuarios/auth", "/usuarios/login", "/usuarios/register",
                                     "/css/**", "/js/**", "/img/**", "/error/**");
    }
}

