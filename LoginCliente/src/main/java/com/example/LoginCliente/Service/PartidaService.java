package com.example.LoginCliente.Service;

import com.example.LoginCliente.Models.DocumentosFuente;
import com.example.LoginCliente.Models.DocumentosPartida;
import com.example.LoginCliente.Models.Partida;
import com.example.LoginCliente.Models.Movimiento;
import com.example.LoginCliente.Repository.DocumentosFuenteRepository;
import com.example.LoginCliente.Repository.DocumentosPartidaRepository;
import com.example.LoginCliente.Repository.PartidaRepository;
import com.example.LoginCliente.Repository.MovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
public class PartidaService {

    @Autowired
    private PartidaRepository partidaRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private DocumentosFuenteRepository documentosFuenteRepository;

    @Autowired
    private DocumentosPartidaRepository documentosPartidaRepository;

    public List<Partida> findAll() {
        return partidaRepository.findAllByOrderByIdPartidaAsc();
    }

    public Optional<Partida> findById(Integer id) {
        return partidaRepository.findById(id);
    }

    @Transactional
    public Partida save(Partida partida, List<Movimiento> movimientos, List<DocumentosFuente> documentosFuentes) {
        Partida savedPartida = partidaRepository.save(partida);

        for (Movimiento movimiento : movimientos) {
            movimiento.setIdPartida(savedPartida.getId_partida());
            movimientoRepository.save(movimiento);
        }
        documentosFuenteRepository.saveAll(documentosFuentes);

        for (DocumentosFuente documento : documentosFuentes) {
            DocumentosPartida documentosPartida = new DocumentosPartida();
            documentosPartida.setDocumento(documento);
            documentosPartida.setPartida(savedPartida);
            documentosPartidaRepository.save(documentosPartida);
        }

        return savedPartida;
    }

    public List<Partida> findByFechaBetween(Timestamp fechaInicio, Timestamp fechaFin) {
        return partidaRepository.findByFechaBetween(fechaInicio, fechaFin);
    }

    public List<Movimiento> findMovimientosByPartida(Integer idPartida) {
        return movimientoRepository.findByIdPartida(idPartida);
    }
}