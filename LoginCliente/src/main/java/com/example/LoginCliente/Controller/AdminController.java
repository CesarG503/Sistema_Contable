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
        return "admin/usuarios";
    }

    @PostMapping("/usuario/guardar")
    public String guardarUsuario(@RequestParam(required = false) Integer id_usuario,
                                  @RequestParam String usuario,
                                  @RequestParam String correo,
                                  @RequestParam(required = false) String pwd,
                                  @RequestParam Integer permiso,
                                  Model model) {
        try {
            Usuario usuarioObj;

            if (id_usuario != null && id_usuario > 0) {
                // Editar usuario existente
                Optional<Usuario> usuarioExistente = usuarioService.findById(id_usuario);
                if (usuarioExistente.isPresent()) {
                    usuarioObj = usuarioExistente.get();
                    String pwdAnterior = usuarioObj.getPwd(); // Guardar contraseña anterior

                    usuarioObj.setUsuario(usuario);
                    usuarioObj.setCorreo(correo);
                    usuarioObj.setPermiso(Permiso.valueOfValor(permiso));

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
                usuarioObj.setPermiso(Permiso.valueOfValor(permiso));
                usuarioService.save(usuarioObj); // Esto codificará la contraseña
            }

            return "redirect:/admin/usuario?success=true";

        } catch (Exception e) {
            logger.severe("Error al guardar usuario: " + e.getMessage());
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

            usuarioService.deleteById(id);
            return "redirect:/admin/usuario?success=deleted";

        } catch (Exception e) {
            logger.severe("Error al eliminar usuario: " + e.getMessage());
            return "redirect:/admin/usuario?error=delete";
        }
    }
}
