package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Models.Usuario;
import com.example.LoginCliente.Service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.logging.Logger;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    private static final Logger logger = Logger.getLogger(UsuarioController.class.getName());

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Prevent binding of id_usuario to avoid type conversion errors
        binder.setDisallowedFields("id_usuario");

        // Trim strings to remove whitespace
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        Usuario usuario = new Usuario();
        model.addAttribute("usuario", usuario);
        return "register";
    }

    @PostMapping("/register")
    public String registerUsuario(@RequestParam("usuario") String username,
                                  @RequestParam("pwd") String password,
                                  Model model) {
        logger.info("Attempting to register user: " + username);

        // Validate input
        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("errorMsg", "Usuario es obligatorio");
            model.addAttribute("usuario", new Usuario());
            return "register";
        }

        if (password == null || password.length() < 8) {
            model.addAttribute("errorMsg", "Contraseña debe tener al menos 8 caracteres");
            model.addAttribute("usuario", new Usuario());
            return "register";
        }

        // Check if user already exists
        Usuario existingUsuario = usuarioService.findByUsuario(username);
        if (existingUsuario != null) {
            model.addAttribute("errorMsg", "El usuario ya está registrado");
            model.addAttribute("usuario", new Usuario());
            logger.warning("Usuario already registered: " + username);
            return "register";
        }

        try {
            // Create new usuario manually
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setUsuario(username.trim());
            nuevoUsuario.setPwd(password);
            nuevoUsuario.setPermiso(1); // Default permission

            usuarioService.save(nuevoUsuario);
            logger.info("Usuario registered successfully: " + username);
            return "redirect:/usuarios/login";
        } catch (Exception e) {
            logger.severe("Error inesperado al registrar usuario: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMsg", "Ocurrió un error inesperado. Intenta nuevamente.");
            model.addAttribute("usuario", new Usuario());
            return "register";
        }
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "login";
    }

    @GetMapping("/home")
    public String home(Model model) {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Usuario usuario = usuarioService.findByUsuario(username);
        model.addAttribute("usuario", usuario);

        return "dashboard";
    }
}
