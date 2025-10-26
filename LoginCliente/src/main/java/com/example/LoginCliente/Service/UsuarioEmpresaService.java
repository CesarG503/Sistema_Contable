package com.example.LoginCliente.Service;

import com.example.LoginCliente.Models.UsuarioEmpresa;
import com.example.LoginCliente.Repository.UsuarioEmpresaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioEmpresaService {

    @Autowired
    private UsuarioEmpresaRepository usuarioEmpresaRepository;

    public List<UsuarioEmpresa> findAll() {
        return usuarioEmpresaRepository.findAll();
    }

    public Optional<UsuarioEmpresa> findById(Integer id) {
        return usuarioEmpresaRepository.findById(id);
    }

    public UsuarioEmpresa save(UsuarioEmpresa usuarioEmpresa) {
        if (usuarioEmpresa.getFechaAfiliacion() == null) {
            usuarioEmpresa.setFechaAfiliacion(new Timestamp(System.currentTimeMillis()));
        }
        return usuarioEmpresaRepository.save(usuarioEmpresa);
    }

    public List<UsuarioEmpresa> findByIdUsuario(Integer idUsuario) {
        return usuarioEmpresaRepository.findByIdUsuario(idUsuario);
    }

    public UsuarioEmpresa findByIdUsuarioAndIdEmpresa(Integer idUsuario, Integer idEmpresa) {
        return usuarioEmpresaRepository.findByIdUsuarioAndIdEmpresa(idUsuario, idEmpresa);
    }

    public List<UsuarioEmpresa> findEmpresasByUsuario(Integer idUsuario) {
        return usuarioEmpresaRepository.findEmpresasByUsuario(idUsuario);
    }

    public List<UsuarioEmpresa> findByIdEmpresa(Integer idEmpresa) {
        return usuarioEmpresaRepository.findByIdEmpresa(idEmpresa);
    }

    public void delete(Integer id) {
        usuarioEmpresaRepository.deleteById(id);
    }
}
