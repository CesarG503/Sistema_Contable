package com.example.LoginCliente.Service;

import com.example.LoginCliente.Models.Permiso;
import com.example.LoginCliente.Models.Usuario;
import com.example.LoginCliente.Models.UsuarioEmpresa;
import com.example.LoginCliente.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class UsuarioService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Logger logger = Logger.getLogger(UsuarioService.class.getName());

    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> findById(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario);
    }

    public Usuario save(Usuario usuario) {
        usuario.setPwd(passwordEncoder.encode(usuario.getPwd()));
        return usuarioRepository.save(usuario);
    }

    public void deleteById(Integer idUsuario) {
        usuarioRepository.deleteById(idUsuario);
    }

    public Usuario findByUsuario(String usuario) {
        return usuarioRepository.findByUsuario(usuario);
    }

    public Usuario findByCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo);
    }

    public boolean verificarContrasena(String contrasenaNoCodificada, String contrasenaCodificada) {
        return passwordEncoder.matches(contrasenaNoCodificada, contrasenaCodificada);
    }

    /**
     * Genera un token único de recuperación y lo asigna al usuario
     * El token expira en 24 horas
     */
    public String generarTokenRecuperacion(Usuario usuario) {
        String token = UUID.randomUUID().toString();
        usuario.setTokenRecuperacion(token);
        usuario.setTokenExpiracion(LocalDateTime.now().plusHours(24));
        usuarioRepository.save(usuario);
        return token;
    }

    /**
     * Valida si el token es válido y no ha expirado
     */
    public Usuario validarToken(String token) {
        Usuario usuario = usuarioRepository.findByTokenRecuperacion(token);

        if (usuario == null) {
            return null;
        }

        // Verificar si el token ha expirado
        if (usuario.getTokenExpiracion() == null ||
            LocalDateTime.now().isAfter(usuario.getTokenExpiracion())) {
            return null;
        }

        return usuario;
    }

    /**
     * Cambia la contraseña del usuario y elimina el token
     */
    public void cambiarContrasenaConToken(String token, String nuevaContrasena) {
        Usuario usuario = validarToken(token);
        if (usuario == null) {
            throw new IllegalArgumentException("Token inválido o expirado");
        }

        usuario.setPwd(passwordEncoder.encode(nuevaContrasena));
        usuario.setTokenRecuperacion(null);
        usuario.setTokenExpiracion(null);
        usuarioRepository.save(usuario);
    }

    @Override
    public UserDetails loadUserByUsername(String usuario) throws UsernameNotFoundException {
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IllegalArgumentException("Usuario no puede ser nulo o vacío");
        }

        Usuario user = usuarioRepository.findByUsuario(usuario);

        if (user == null) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }

        if (user.getUsuario() == null || user.getUsuario().isEmpty() ||
                user.getPwd() == null || user.getPwd().isEmpty()) {
            throw new IllegalArgumentException("Usuario o contraseña no pueden ser nulos o vacíos");
        }

        // No asignamos roles/permissions aquí: la autenticación solo verifica existencia y contraseña.
        // Devolvemos un UserDetails con authorities vacías.
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsuario())
                .password(user.getPwd())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_User")))
                .build();
    }
}
