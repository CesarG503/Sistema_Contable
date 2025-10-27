package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Models.Usuario;
import com.example.LoginCliente.Service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        binder.setDisallowedFields("idUsuario");

        // Trim strings to remove whitespace
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/auth")
    public String authPage(@RequestParam(value = "panel", defaultValue = "login") String panel,
                           @RequestParam(value = "loginErr", required = false) String loginErr,
                           @RequestParam(value = "registered", required = false) String registered,
                           Model model) {

        model.addAttribute("activePanel", panel);

        if (registered != null) {
            model.addAttribute("loginMsg", "Registro exitoso. Inicia sesión.");
        }
        if (loginErr != null) {
            // Opción A (genérico para no revelar si el usuario existe):
            String msg = "Usuario o contraseña incorrectos.";
            // Opción B (diferenciar):
            if ("nouser".equals(loginErr)) {
                msg = "El usuario no existe.";
            } else if ("bad".equals(loginErr)) {
                msg = "Usuario o contraseña incorrectos.";
            }
            model.addAttribute("loginError", msg);
        }
        return "auth";
    }


    // Validación previa al login (igual estilo que registro)
    @PostMapping("/login-validate")
    public String loginValidate(@RequestParam("username") String username,
                                @RequestParam("password") String password,
                                Model model) {
        model.addAttribute("activePanel", "login");

        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("loginError", "El usuario es obligatorio.");
            return "auth";
        }
        if (password == null || password.isEmpty()) {
            model.addAttribute("loginError", "La contraseña es obligatoria.");
            return "auth";
        }
        if (password.length() < 4) {
            model.addAttribute("loginError", "La contraseña es muy corta.");
            return "auth";
        }

        // Continúa el flujo normal de Spring Security
        return "forward:/login";
    }

    @GetMapping("/login")
    public String legacyLogin() {
        return "redirect:/usuarios/auth?panel=login";
    }

    @GetMapping("/register")
    public String legacyRegister() {
        return "redirect:/usuarios/auth?panel=register";
    }

    @PostMapping("/register")
    public String registerUsuario(@RequestParam("usuario") String username,
                                  @RequestParam("correo") String correo,
                                  @RequestParam("pwd") String password,
                                  @RequestParam("password2") String password2,
                                  Model model) {
        model.addAttribute("activePanel", "register");
        model.addAttribute("usuario", new Usuario());


//        validar si ya hay mas de un usuario registrado
        List<Usuario> usuarios = usuarioService.findAll();
        if ( usuarios.size() >= 1) {
            model.addAttribute("registerError", "El registro está cerrado. Para poder registrarte no debe de haber nadie mas en la dba.");
            return "auth";
        }

        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("registerError", "El usuario es obligatorio.");
            return "auth";
        }
        if (correo == null || correo.trim().isEmpty()) {
            model.addAttribute("registerError", "El correo es obligatorio.");
            return "auth";
        }
        if (password == null || password.isEmpty()) {
            model.addAttribute("registerError", "La contraseña es obligatoria.");
            return "auth";
        }
        if (password.length() < 8) {
            model.addAttribute("registerError", "La contraseña debe tener al menos 8 caracteres.");
            return "auth";
        }
        if (password2 == null || !password.equals(password2)) {
            model.addAttribute("registerError", "Las contraseñas no coinciden.");
            return "auth";
        }
        Usuario existing = usuarioService.findByUsuario(username.trim());
        if (existing != null) {
            model.addAttribute("registerError", "El usuario ya existe.");
            return "auth";
        }


        try {
            Usuario nuevo = new Usuario();
            nuevo.setUsuario(username.trim());
            nuevo.setCorreo(correo.trim());
            nuevo.setPwd(password);
            usuarioService.save(nuevo);
            return "redirect:/usuarios/auth?panel=login&registered=1";
        } catch (Exception e) {
            logger.severe("Error al registrar: " + e.getMessage());
            model.addAttribute("registerError", "Error inesperado. Intenta nuevamente.");
            return "auth";
        }
    }


    @GetMapping("/home")
    public String home(Model model, HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Usuario usuario = usuarioService.findByUsuario(username);

        session.setAttribute("usuarioNombre", usuario.getUsuario());
        session.setAttribute("usuarioCorreo", usuario.getCorreo());

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
