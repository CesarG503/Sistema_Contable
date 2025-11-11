package com.example.LoginCliente.Repository;

import com.example.LoginCliente.Models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Usuario findByUsuario(String usuario);
    Usuario findByCorreo(String correo);
    Usuario findByTokenRecuperacion(String token);
}

