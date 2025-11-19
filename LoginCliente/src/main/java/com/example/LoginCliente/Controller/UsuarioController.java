package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Models.Usuario;
import com.example.LoginCliente.Service.EmailService;
import com.example.LoginCliente.Service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import jdk.jfr.Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EmailService emailService;

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

    @PostMapping("/recuperar-cuenta")
    public String recuperarCuenta(@RequestParam("email") String email,
                                  RedirectAttributes redirectAttributes) {
        if (email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El email es obligatorio.");
            return "redirect:/usuarios/recuperar";
        }

        // Buscar usuario por correo
        Usuario usuario = usuarioService.findByCorreo(email.trim());

        if (usuario == null) {
            // Por seguridad, no revelamos si el correo existe o no
            redirectAttributes.addFlashAttribute("success",
                "Si el correo existe, recibirás un enlace de recuperación.");
            return "redirect:/usuarios/recuperar";
        }

        try {
            // Generar token de recuperación
            String token = usuarioService.generarTokenRecuperacion(usuario);

            // Construir enlace de recuperación
            String enlaceRecuperacion = "http://localhost:8080/usuarios/restablecer-pwd?token=" + token;

            // Preparar variables para el template
            Map<String, Object> variables = new HashMap<>();
            variables.put("enlace", enlaceRecuperacion);
            variables.put("nombreUsuario", usuario.getUsuario());

            // Enviar correo con el template
            emailService.sendEmailHtml(
                email.trim(),
                "OneDI System",
                "auth/email-pwd",
                variables
            );

            redirectAttributes.addFlashAttribute("success",
                "Si el correo existe, recibirás un enlace de recuperación.");
        } catch (Exception e) {
            logger.severe("Error al enviar correo de recuperación: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                "Hubo un error al procesar tu solicitud. Intenta nuevamente.");
        }

        return "redirect:/usuarios/recuperar";
    }

    @GetMapping("/recuperar")
    public String recuperar(Model model) {


        return "auth/recuperar";
    }

    /**
     * Endpoint que recibe el token y muestra el formulario para cambiar contraseña
     */
    @GetMapping("/restablecer-pwd")
    public String restablecerPassword(@RequestParam("token") String token, Model model) {
        Usuario usuario = usuarioService.validarToken(token);

        if (usuario == null) {
            model.addAttribute("error", "El enlace es inválido o ha expirado.");
            return "auth/recuperar";
        }

        model.addAttribute("token", token);
        model.addAttribute("usuario", usuario.getUsuario());
        return "auth/restablecer-pwd";
    }

    /**
     * Endpoint para procesar el cambio de contraseña
     */
    @PostMapping("/restablecer-pwd")
    public String procesarRestablecerPassword(@RequestParam("token") String token,
                                              @RequestParam("password") String password,
                                              @RequestParam("password2") String password2,
                                              RedirectAttributes redirectAttributes) {

        if (password == null || password.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La contraseña es obligatoria.");
            redirectAttributes.addAttribute("token", token);
            return "redirect:/usuarios/restablecer-pwd";
        }

        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "La contraseña debe tener al menos 8 caracteres.");
            redirectAttributes.addAttribute("token", token);
            return "redirect:/usuarios/restablecer-pwd";
        }

        if (!password.equals(password2)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden.");
            redirectAttributes.addAttribute("token", token);
            return "redirect:/usuarios/restablecer-pwd";
        }

        try {
            usuarioService.cambiarContrasenaConToken(token, password);
            redirectAttributes.addFlashAttribute("success", "Contraseña cambiada exitosamente. Ya puedes iniciar sesión.");
            return "redirect:/usuarios/auth?panel=login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "El enlace es inválido o ha expirado.");
            return "redirect:/usuarios/recuperar";
        } catch (Exception e) {
            logger.severe("Error al cambiar contraseña: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al cambiar la contraseña. Intenta nuevamente.");
            redirectAttributes.addAttribute("token", token);
            return "redirect:/usuarios/restablecer-pwd";
        }
    }
}
