package com.example.LoginCliente.Repository;

import com.example.LoginCliente.Models.UsuarioEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, Integer> {

    List<UsuarioEmpresa> findByIdUsuario(Integer idUsuario);

    UsuarioEmpresa findByIdUsuarioAndIdEmpresa(Integer idUsuario, Integer idEmpresa);

    @Query("SELECT ue FROM UsuarioEmpresa ue WHERE ue.idUsuario = :idUsuario")
    List<UsuarioEmpresa> findEmpresasByUsuario(@Param("idUsuario") Integer idUsuario);
}
