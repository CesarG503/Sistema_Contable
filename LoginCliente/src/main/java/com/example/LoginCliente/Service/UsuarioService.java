package com.example.LoginCliente.Service;

import com.example.LoginCliente.Models.Usuario;
import com.example.LoginCliente.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
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

    public Optional<Usuario> findById(Integer id_usuario) {
        return usuarioRepository.findById(id_usuario);
    }

    public Usuario save(Usuario usuario) {
        usuario.setPwd(passwordEncoder.encode(usuario.getPwd()));
        return usuarioRepository.save(usuario);
    }

    public void deleteById(Integer id_usuario) {
        usuarioRepository.deleteById(id_usuario);
    }

    public Usuario findByUsuario(String usuario) {
        return usuarioRepository.findByUsuario(usuario);
    }

    public boolean verificarContrasena(String contrasenaNoCodificada, String contrasenaCodificada) {
        return passwordEncoder.matches(contrasenaNoCodificada, contrasenaCodificada);
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

        return org.springframework.security.core.userdetails.User.withUsername(user.getUsuario())
                .password(user.getPwd())
                .authorities("USER")
                .build();
    }
}
