package com.example.LoginCliente.Security;

import com.example.LoginCliente.Models.Usuario;
import com.example.LoginCliente.Service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserSessionInterceptor implements HandlerInterceptor {

    @Autowired
    private UsuarioService usuarioService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Solo procesar si hay un usuario autenticado y no es anónimo
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {

            HttpSession session = request.getSession();

            // Solo consultar la DB si no existe en sesión
            if (session.getAttribute("usuarioId") == null) {
                String username = authentication.getName();
                Usuario usuario = usuarioService.findByUsuario(username);

                if (usuario != null) {
                    // Guardar datos del usuario en la sesión
                    session.setAttribute("usuarioId", usuario.getId_usuario());
                    session.setAttribute("usuarioNombre", usuario.getUsuario());
                    session.setAttribute("usuarioCorreo", usuario.getCorreo());
                    session.setAttribute("usuarioRol", usuario.getPermiso().texto);
                    session.setAttribute("usuarioRolValor", usuario.getPermiso().valor);
                    session.setAttribute("esAdmin", usuario.getPermiso().valor == 0);
                }
            }
        }

        return true;
    }
}

