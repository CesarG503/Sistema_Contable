package com.example.LoginCliente.Repository;

import com.example.LoginCliente.Models.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento, Integer> {

    // Buscar movimientos por ID de partida
    List<Movimiento> findByIdPartida(Integer idPartida);

    // Buscar movimientos por ID de cuenta
    @Query("SELECT m FROM Movimiento m WHERE m.idCuenta = ?1")
    List<Movimiento> findByIdCuenta(Integer idCuenta);

    // Eliminar movimientos por ID de partida (usa este)
    @Modifying
    @Transactional
    @Query("DELETE FROM Movimiento m WHERE m.idPartida = :idPartida")
    void deleteByIdPartida(@Param("idPartida") Integer idPartida);
}