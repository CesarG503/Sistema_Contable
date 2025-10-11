package com.example.LoginCliente.Repository;

import com.example.LoginCliente.Models.DocumentosFuente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentosFuenteRepository extends JpaRepository<DocumentosFuente, Integer> {

}
