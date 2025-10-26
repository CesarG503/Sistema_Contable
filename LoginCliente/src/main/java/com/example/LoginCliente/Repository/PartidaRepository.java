package com.example.LoginCliente.Repository;

import com.example.LoginCliente.Models.Partida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface PartidaRepository extends JpaRepository<Partida, Integer> {
    List<Partida> findByFechaBetween(Timestamp fechaInicio, Timestamp fechaFin);
    List<Partida> findAllByOrderByFechaDesc();
    List<Partida> findAllByOrderByIdPartidaAsc();

    List<Partida> findByIdEmpresa(Integer idEmpresa);
    List<Partida> findByIdEmpresaOrderByIdPartidaAsc(Integer idEmpresa);
}
