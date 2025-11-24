package com.example.LoginCliente.Service;

import com.example.LoginCliente.Models.Reporte;
import com.example.LoginCliente.Repository.ReporteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReporteService {

    @Autowired
    private ReporteRepository reporteRepository;

    public Reporte save(Reporte reporte) {
        return reporteRepository.save(reporte);
    }

    public List<Reporte> findByIdEmpresa(Integer idEmpresa) {
        return reporteRepository.findReportesByIdEmpresa(idEmpresa);
    }

    public Optional<Reporte> findById(Integer id) {
        return reporteRepository.findById(id);
    }

    public void deleteById(Integer id) {
        reporteRepository.deleteById(id);
    }
}