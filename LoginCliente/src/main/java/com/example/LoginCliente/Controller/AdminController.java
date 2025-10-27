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
        binder.setDisallowedFields("idUsuario");

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
    public String guardarUsuario(@RequestParam(required = false) Integer idUsuario,
                                 @RequestParam String usuario,
                                 @RequestParam String correo,
                                 @RequestParam(required = false) String pwd,
                                 Model model) {
        try {
            Usuario usuarioObj;

            if (idUsuario != null && idUsuario > 0) {
                // Editar usuario existente
                Optional<Usuario> usuarioExistente = usuarioService.findById(idUsuario);
                if (usuarioExistente.isPresent()) {
                    usuarioObj = usuarioExistente.get();
                    String pwdAnterior = usuarioObj.getPwd(); // Guardar contraseña anterior

                    usuarioObj.setUsuario(usuario);
                    usuarioObj.setCorreo(correo);


                    // Solo actualizar contraseña si se proporcionó una nueva
                    if (pwd != null && !pwd.isEmpty()) {
                        usuarioObj.setPwd(pwd);
                        usuarioService.save(usuarioObj); // Esto codificará la nueva contraseña
                    } else {
                        // No cambiar contraseña, guardar directamente sin codificar
                        usuarioObj.setPwd(pwdAnterior);
                        usuarioRepository.save(usuarioObj);
                    }
                } else {
                    return "redirect:/admin/usuario?error=notfound";
                }
            } else {
                // Crear nuevo usuario
                usuarioObj = new Usuario();
                usuarioObj.setUsuario(usuario);
                usuarioObj.setCorreo(correo);
                usuarioObj.setPwd(pwd);

                usuarioService.save(usuarioObj); // Esto codificará la contraseña
            }

            // Validar que la contraseña tenga al menos 8 caracteres
            if (pwd.length() < 8) {
                return "redirect:/admin/usuario?error=pwdshort";
            }

            // Crear nuevo usuario
            usuarioObj = new Usuario();
            usuarioObj.setUsuario(usuario);
            usuarioObj.setCorreo(correo);
            usuarioObj.setPwd(pwd);
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

                usuarioObj.setUsuario(usuario);
                usuarioObj.setCorreo(correo);

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
            if (usuarioActual != null && usuarioActual.getIdUsuario().equals(id)) {
                return "redirect:/admin/usuario?error=selfdelete";
            }

            usuarioService.deleteById(id);
            return "redirect:/admin/usuario?success=deleted";

        } catch (Exception e) {
            logger.severe("Error al eliminar usuario: " + e.getMessage());
            return "redirect:/admin/usuario?error=delete";
        }
    }
}
