package com.example.LoginCliente.Security;

import com.example.LoginCliente.Models.Usuario;
import com.example.LoginCliente.Models.UsuarioEmpresa;
import com.example.LoginCliente.Service.UsuarioEmpresaService;
import com.example.LoginCliente.Service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Refresca en cada request los atributos de rol/permisos ligados a la empresa activa
 * para evitar valores obsoletos tras cambios de rol. Si la relación del usuario con la
 * empresa cambia (permiso), se refleja inmediatamente sin exigir cerrar sesión.
 */
@Component
public class EmpresaPermisoRefreshInterceptor implements HandlerInterceptor {

    @Autowired
    private UsuarioEmpresaService usuarioEmpresaService;

    @Autowired
    private UsuarioService usuarioService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }
        HttpSession session = request.getSession(false);
        if (session == null) return true;

        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
        if (empresaActiva == null) return true; // No hay empresa seleccionada aún

        Integer usuarioId = (Integer) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            // Cargar usuarioId si por alguna razón aún no se ha cargado
            Usuario usuario = usuarioService.findByUsuario(auth.getName());
            if (usuario != null) {
                usuarioId = usuario.getIdUsuario();
                session.setAttribute("usuarioId", usuarioId);
            } else {
                return true;
            }
        }

        // Buscar relación actualizada
        UsuarioEmpresa rel = usuarioEmpresaService.findByIdUsuarioAndIdEmpresa(usuarioId, empresaActiva);
        if (rel != null) {
            // Actualizar siempre (evita datos obsoletos)
            session.setAttribute("usuarioPermiso", rel.getPermisoValor());
            session.setAttribute("usuarioRol", rel.getPermisoTexto());
        } else {
            // Si ya no tiene relación, limpiar permiso
            session.removeAttribute("usuarioPermiso");
            session.removeAttribute("usuarioRol");
        }
        return true;
    }
}

