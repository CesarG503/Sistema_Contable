package com.example.LoginCliente.Repository;

import com.example.LoginCliente.Models.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {
    Empresa findByNit(String nit);
}
