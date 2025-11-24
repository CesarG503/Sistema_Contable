package com.example.LoginCliente.Repository;

import com.example.LoginCliente.Models.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, Integer> {
    @Query("SELECT r FROM Reporte r WHERE r.empresa.idEmpresa = :idEmpresa")
    List<Reporte> findReportesByIdEmpresa(Integer idEmpresa);
}
