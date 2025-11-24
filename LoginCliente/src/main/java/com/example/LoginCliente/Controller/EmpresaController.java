package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Models.Empresa;
import com.example.LoginCliente.Models.Usuario;
import com.example.LoginCliente.Models.UsuarioEmpresa;
import com.example.LoginCliente.Models.Permiso;
import com.example.LoginCliente.Service.EmpresaService;
import com.example.LoginCliente.Service.UsuarioService;
import com.example.LoginCliente.Service.UsuarioEmpresaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.logging.Logger;

@Controller
@RequestMapping("/empresas")
public class EmpresaController {

    @Autowired
    private EmpresaService empresaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioEmpresaService usuarioEmpresaService;

    private static final Logger logger = Logger.getLogger(EmpresaController.class.getName());

    @GetMapping("/mis-empresas")
    public String misEmpresas(Model model, HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Usuario usuario = usuarioService.findByUsuario(username);

        List<UsuarioEmpresa> usuarioEmpresas = usuarioEmpresaService.findByIdUsuario(usuario.getIdUsuario());

        model.addAttribute("usuario", usuario);
        model.addAttribute("usuarioEmpresas", usuarioEmpresas);
        model.addAttribute("page", "mis-empresas");

        return "mis-empresas";
    }

    @PostMapping("/crear")
    public String crearEmpresa(@RequestParam("nombre") String nombre,
                               @RequestParam("nit") String nit,
                               @RequestParam(value = "direccion", required = false) String direccion,
                               @RequestParam(value = "descripcion", required = false) String descripcion,
                               @RequestParam(value = "telefono", required = false) String telefono,
                               HttpSession session,
                               Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Usuario usuario = usuarioService.findByUsuario(username);

        try {
            // Crear empresa
            Empresa empresa = new Empresa();
            empresa.setNombre(nombre);
            empresa.setNit(nit);
            empresa.setDireccion(direccion);
            empresa.setDescripcion(descripcion);
            empresa.setTelefono(telefono);

            Empresa savedEmpresa = empresaService.save(empresa);

            // Crear relación usuario-empresa con permiso de administrador
            UsuarioEmpresa usuarioEmpresa = new UsuarioEmpresa();
            usuarioEmpresa.setIdUsuario(usuario.getIdUsuario());
            usuarioEmpresa.setIdEmpresa(savedEmpresa.getIdEmpresa());
            usuarioEmpresa.setPermiso(Permiso.Administrador);

            usuarioEmpresaService.save(usuarioEmpresa);

            return "redirect:/empresas/mis-empresas";
        } catch (Exception e) {
            logger.severe("Error al crear empresa: " + e.getMessage());
            model.addAttribute("error", "Error al crear la empresa");
            return "redirect:/empresas/mis-empresas?error=1";
        }
    }

    @GetMapping("/seleccionar/{idEmpresa}")
    public String seleccionarEmpresa(@PathVariable Integer idEmpresa, HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Usuario usuario = usuarioService.findByUsuario(username);

        // Verificar que el usuario tiene acceso a esta empresa
        UsuarioEmpresa usuarioEmpresa = usuarioEmpresaService.findByIdUsuarioAndIdEmpresa(
                usuario.getIdUsuario(), idEmpresa);

        if (usuarioEmpresa != null) {
            session.setAttribute("empresaActiva", idEmpresa);
            session.setAttribute("empresaNombre", usuarioEmpresa.getEmpresa().getNombre());
            session.setAttribute("usuarioEmpresaId", usuarioEmpresa.getIdUsuarioEmpresa());
            session.setAttribute("usuarioRol", usuarioEmpresa.getPermiso().texto);
            session.setAttribute("usuarioPermiso", usuarioEmpresa.getPermiso().valor);

            // Construir authorities dinámicas para la empresa seleccionada
            var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();
            authorities.add(new SimpleGrantedAuthority("ROLE_User"));
            switch (usuarioEmpresa.getPermiso()) {
                case Administrador -> authorities.add(new SimpleGrantedAuthority("ROLE_Administrador"));
                case Auditor -> authorities.add(new SimpleGrantedAuthority("ROLE_Auditor"));
                case Contador -> authorities.add(new SimpleGrantedAuthority("ROLE_Contador"));
            }
            // Reemplazar Authentication en el contexto de seguridad
            var newAuth = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), authorities);
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }

        return "redirect:/dashboard";
    }
}
