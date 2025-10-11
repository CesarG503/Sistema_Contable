package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Models.Permiso;
import com.example.LoginCliente.Models.Usuario;
import com.example.LoginCliente.Repository.UsuarioRepository;
import com.example.LoginCliente.Service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private static final Logger logger = Logger.getLogger(UsuarioController.class.getName());

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Prevent binding of id_usuario to avoid type conversion errors
        binder.setDisallowedFields("id_usuario");

        // Trim strings to remove whitespace
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @RequestMapping("/")
    public String admin() {
        return "admin/admin";
    }

    @GetMapping("/usuario")
    public String usuarios(Model model) {
        // Obtener todos los usuarios
        List<Usuario> usuarios = usuarioService.findAll();
        model.addAttribute("usuarios", usuarios);

        // Obtener el usuario actual para validaciones en el frontend
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Usuario usuarioActual = usuarioService.findByUsuario(username);
        model.addAttribute("usuarioActual", usuarioActual);

        return "admin/usuarios";
    }

    @PostMapping("/usuario/guardar")
    public String guardarUsuario(@RequestParam String usuario,
                                  @RequestParam String correo,
                                  @RequestParam String pwd,
                                  @RequestParam String pwd2,
                                  @RequestParam Integer permiso) {
        try {
            // Validar que las contraseñas coincidan
            if (!pwd.equals(pwd2)) {
                return "redirect:/admin/usuario?error=pwdmismatch";
            }

            // Validar que la contraseña tenga al menos 8 caracteres
            if (pwd.length() < 8) {
                return "redirect:/admin/usuario?error=pwdshort";
            }

            // Crear nuevo usuario
            Usuario usuarioObj = new Usuario();
            usuarioObj.setUsuario(usuario);
            usuarioObj.setCorreo(correo);
            usuarioObj.setPwd(pwd);
            usuarioObj.setPermiso(Permiso.valueOfValor(permiso));
            usuarioService.save(usuarioObj); // Esto codificará la contraseña

            return "redirect:/admin/usuario?success=true";

        } catch (Exception e) {
            logger.severe("Error al guardar usuario: " + e.getMessage());
            return "redirect:/admin/usuario?error=save";
        }
    }

    @PostMapping("/usuario/editar")
    public String editarUsuario(@RequestParam Integer id_usuario,
                                 @RequestParam String usuario,
                                 @RequestParam String correo,
                                 @RequestParam Integer permiso,
                                 HttpServletRequest request,
                                 Model model) {
        try {
            // VALIDACIÓN CRÍTICA: Rechazar si intentan enviar campos de contraseña
            if (request.getParameter("pwd") != null ||
                request.getParameter("pwd2") != null) {
                logger.warning("Intento de modificar contraseña en edición. Usuario: " + usuario);
                return "redirect:/admin/usuario?error=forbidden";
            }

            // Obtener el usuario actual autenticado
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String usernameActual = authentication.getName();
            Usuario usuarioActual = usuarioService.findByUsuario(usernameActual);

            // Obtener el usuario que se está intentando editar
            Optional<Usuario> usuarioExistente = usuarioService.findById(id_usuario);
            if (usuarioExistente.isPresent()) {
                Usuario usuarioObj = usuarioExistente.get();

                // VALIDACIÓN: NADIE puede editar a un administrador (ni siquiera otro admin)
                if (usuarioObj.getPermiso().valor == 0) {
                    logger.warning("Intento de editar administrador. Usuario actual: " + usernameActual);
                    return "redirect:/admin/usuario?error=nopermission";
                }

                usuarioObj.setUsuario(usuario);
                usuarioObj.setCorreo(correo);
                usuarioObj.setPermiso(Permiso.valueOfValor(permiso));

                // Guardar directamente con el repositorio para no recodificar la contraseña
                usuarioRepository.save(usuarioObj);

                return "redirect:/admin/usuario?success=edited";
            } else {
                return "redirect:/admin/usuario?error=notfound";
            }

        } catch (Exception e) {
            logger.severe("Error al editar usuario: " + e.getMessage());
            return "redirect:/admin/usuario?error=save";
        }
    }

    @GetMapping("/usuario/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Integer id, Model model) {
        try {
            // No permitir eliminar al usuario actual
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            Usuario usuarioActual = usuarioService.findByUsuario(username);

            // Validar que el usuario actual existe
            if (usuarioActual != null && usuarioActual.getId_usuario().equals(id)) {
                return "redirect:/admin/usuario?error=selfdelete";
            }

            // Obtener el usuario que se quiere eliminar
            Optional<Usuario> usuarioAEliminar = usuarioService.findById(id);
            if (usuarioAEliminar.isPresent()) {
                // VALIDACIÓN: NADIE puede eliminar a un administrador (ni siquiera otro admin)
                if (usuarioAEliminar.get().getPermiso().valor == 0) {
                    logger.warning("Intento de eliminar administrador. Usuario actual: " + username);
                    return "redirect:/admin/usuario?error=nopermission";
                }
            }

            usuarioService.deleteById(id);
            return "redirect:/admin/usuario?success=deleted";

        } catch (Exception e) {
            logger.severe("Error al eliminar usuario: " + e.getMessage());
            return "redirect:/admin/usuario?error=delete";
        }
    }
}
