package com.example.LoginCliente.Repository;

import com.example.LoginCliente.Models.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento, Integer> {
    List<Movimiento> findByIdPartida(Integer idPartida);

    @Query("SELECT m FROM Movimiento m WHERE m.idCuenta = ?1")
    List<Movimiento> findByIdCuenta(Integer idCuenta);

    @Query("SELECT m FROM Movimiento m JOIN Partida p ON m.idPartida = p.idPartida WHERE m.idCuenta = ?1 AND p.fecha BETWEEN ?2 AND ?3")
    List<Movimiento> findByIdCuentaAndDateRange(Integer idCuenta, java.sql.Timestamp fechaInicio, java.sql.Timestamp fechaFin);
}
