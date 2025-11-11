package com.example.LoginCliente.Security;

import com.example.LoginCliente.Service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomAuthFailureHandler failureHandler;

    @Autowired
    private CustomAuthSuccessHandler successHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/usuarios/login",
                                "/usuarios/register",
                                "/usuarios/auth",
                                "usuarios/restablecer-pwd",
                                "/usuarios/login-validate",
                                "/usuarios/recuperar",
                                "/usuarios/recuperar-cuenta",
                                "/error",
                                "/error/**",
                                "/img/**",
                                "/css/**",
                                "/js/**"
                        ).permitAll()
                        // Permission checks should be done at the controller level based on UsuarioEmpresa.permiso
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/usuarios/auth")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .loginProcessingUrl("/login")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/usuarios/auth?panel=login")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/error/403")
                )
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(usuarioService).passwordEncoder(passwordEncoder);
    }
}
