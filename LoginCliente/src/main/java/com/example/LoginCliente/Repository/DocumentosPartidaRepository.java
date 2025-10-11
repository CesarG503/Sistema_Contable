package com.example.LoginCliente.Repository;

import com.example.LoginCliente.Models.DocumentosPartida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentosPartidaRepository extends JpaRepository<DocumentosPartida, Integer> {
}
