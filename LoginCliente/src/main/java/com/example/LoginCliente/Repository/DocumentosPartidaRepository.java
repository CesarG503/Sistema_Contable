package com.example.LoginCliente.Repository;

import com.example.LoginCliente.Models.DocumentosFuente;
import com.example.LoginCliente.Models.DocumentosPartida;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentosPartidaRepository extends JpaRepository<DocumentosPartida, Integer> {
    List<DocumentosPartida> findDocumentosByPartidaIdPartida(Integer partidaIdPartida, Limit limit);
}
