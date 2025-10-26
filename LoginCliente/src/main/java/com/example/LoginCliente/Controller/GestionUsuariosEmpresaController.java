package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Models.*;
import com.example.LoginCliente.Service.UsuarioEmpresaService;
import com.example.LoginCliente.Service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/empresas/usuarios")
public class GestionUsuariosEmpresaController {

    @Autowired
    private UsuarioEmpresaService usuarioEmpresaService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public String gestionUsuarios(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        // Validate company is selected
        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
        if (empresaActiva == null) {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar una empresa primero");
            return "redirect:/empresas/mis-empresas";
        }

        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Usuario usuario = usuarioService.findByUsuario(username);

        // Validate user is admin of the company
        UsuarioEmpresa usuarioEmpresa = usuarioEmpresaService.findByIdUsuarioAndIdEmpresa(
                usuario.getIdUsuario(), empresaActiva);

        if (usuarioEmpresa == null || usuarioEmpresa.getPermiso() != Permiso.Administrador) {
            redirectAttributes.addFlashAttribute("error", "No tiene permisos de administrador para esta empresa");
            return "redirect:/dashboard";
        }

        // Get all users of the company
        List<UsuarioEmpresa> usuariosEmpresa = usuarioEmpresaService.findByIdEmpresa(empresaActiva);

        model.addAttribute("usuariosEmpresa", usuariosEmpresa);
        model.addAttribute("page", "gestion-usuarios");
        model.addAttribute("permisos", Permiso.values());

        return "gestion-usuarios-empresa";
    }

    @PostMapping("/invitar")
    public String invitarUsuario(
            @RequestParam String usernameInvitado,
            @RequestParam Integer permiso,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
        if (empresaActiva == null) {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar una empresa primero");
            return "redirect:/empresas/mis-empresas";
        }

        // Validate current user is admin
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Usuario usuarioActual = usuarioService.findByUsuario(username);

        UsuarioEmpresa usuarioEmpresaActual = usuarioEmpresaService.findByIdUsuarioAndIdEmpresa(
                usuarioActual.getIdUsuario(), empresaActiva);

        if (usuarioEmpresaActual == null || usuarioEmpresaActual.getPermiso() != Permiso.Administrador) {
            redirectAttributes.addFlashAttribute("error", "No tiene permisos de administrador");
            return "redirect:/empresas/usuarios";
        }

        // Find user to invite
        Usuario usuarioInvitado = usuarioService.findByUsuario(usernameInvitado);
        if (usuarioInvitado == null) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
            return "redirect:/empresas/usuarios";
        }

        // Check if user is already in the company
        UsuarioEmpresa existente = usuarioEmpresaService.findByIdUsuarioAndIdEmpresa(
                usuarioInvitado.getIdUsuario(), empresaActiva);

        if (existente != null) {
            redirectAttributes.addFlashAttribute("error", "El usuario ya pertenece a esta empresa");
            return "redirect:/empresas/usuarios";
        }

        // Create new user-company relationship
        UsuarioEmpresa nuevaRelacion = new UsuarioEmpresa();
        nuevaRelacion.setIdUsuario(usuarioInvitado.getIdUsuario());
        nuevaRelacion.setIdEmpresa(empresaActiva);
        nuevaRelacion.setPermiso(Permiso.valueOfValor(permiso));

        usuarioEmpresaService.save(nuevaRelacion);

        redirectAttributes.addFlashAttribute("success", "Usuario invitado exitosamente");
        return "redirect:/empresas/usuarios";
    }

    @PostMapping("/{id}/cambiar-rol")
    public String cambiarRol(
            @PathVariable Integer id,
            @RequestParam Integer nuevoPermiso,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
        if (empresaActiva == null) {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar una empresa primero");
            return "redirect:/empresas/mis-empresas";
        }

        // Validate current user is admin
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Usuario usuarioActual = usuarioService.findByUsuario(username);

        UsuarioEmpresa usuarioEmpresaActual = usuarioEmpresaService.findByIdUsuarioAndIdEmpresa(
                usuarioActual.getIdUsuario(), empresaActiva);

        if (usuarioEmpresaActual == null || usuarioEmpresaActual.getPermiso() != Permiso.Administrador) {
            redirectAttributes.addFlashAttribute("error", "No tiene permisos de administrador");
            return "redirect:/empresas/usuarios";
        }

        // Update user role
        UsuarioEmpresa usuarioEmpresa = usuarioEmpresaService.findById(id).orElse(null);
        if (usuarioEmpresa == null || !usuarioEmpresa.getIdEmpresa().equals(empresaActiva)) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado en esta empresa");
            return "redirect:/empresas/usuarios";
        }

        usuarioEmpresa.setPermiso(Permiso.valueOfValor(nuevoPermiso));
        usuarioEmpresaService.save(usuarioEmpresa);

        redirectAttributes.addFlashAttribute("success", "Rol actualizado exitosamente");
        return "redirect:/empresas/usuarios";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminarUsuario(
            @PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
        if (empresaActiva == null) {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar una empresa primero");
            return "redirect:/empresas/mis-empresas";
        }

        // Validate current user is admin
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Usuario usuarioActual = usuarioService.findByUsuario(username);

        UsuarioEmpresa usuarioEmpresaActual = usuarioEmpresaService.findByIdUsuarioAndIdEmpresa(
                usuarioActual.getIdUsuario(), empresaActiva);

        if (usuarioEmpresaActual == null || usuarioEmpresaActual.getPermiso() != Permiso.Administrador) {
            redirectAttributes.addFlashAttribute("error", "No tiene permisos de administrador");
            return "redirect:/empresas/usuarios";
        }

        // Validate user to delete
        UsuarioEmpresa usuarioEmpresa = usuarioEmpresaService.findById(id).orElse(null);
        if (usuarioEmpresa == null || !usuarioEmpresa.getIdEmpresa().equals(empresaActiva)) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado en esta empresa");
            return "redirect:/empresas/usuarios";
        }

        // Prevent deleting yourself
        if (usuarioEmpresa.getIdUsuario().equals(usuarioActual.getIdUsuario())) {
            redirectAttributes.addFlashAttribute("error", "No puede eliminarse a sí mismo de la empresa");
            return "redirect:/empresas/usuarios";
        }

        usuarioEmpresaService.delete(id);

        redirectAttributes.addFlashAttribute("success", "Usuario eliminado de la empresa");
        return "redirect:/empresas/usuarios";
    }
}
