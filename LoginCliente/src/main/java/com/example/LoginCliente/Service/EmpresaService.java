package com.example.LoginCliente.Service;

import com.example.LoginCliente.Models.Empresa;
import com.example.LoginCliente.Repository.EmpresaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
public class EmpresaService {

    @Autowired
    private EmpresaRepository empresaRepository;

    public List<Empresa> findAll() {
        return empresaRepository.findAll();
    }

    public Optional<Empresa> findById(Integer id) {
        return empresaRepository.findById(id);
    }

    public Empresa save(Empresa empresa) {
        if (empresa.getFechaRegistro() == null) {
            empresa.setFechaRegistro(new Timestamp(System.currentTimeMillis()));
        }
        return empresaRepository.save(empresa);
    }

    public void deleteById(Integer id) {
        empresaRepository.deleteById(id);
    }

    public Empresa findByNit(String nit) {
        return empresaRepository.findByNit(nit);
    }
}
