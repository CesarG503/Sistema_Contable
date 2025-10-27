package com.example.LoginCliente.Repository;

import com.example.LoginCliente.Models.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {
    List<Cuenta> findByIdEmpresa(Integer idEmpresa);
}
