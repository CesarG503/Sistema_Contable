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

    public List<Partida> findByIdEmpresa(Integer idEmpresa) {
        return partidaRepository.findByIdEmpresaOrderByIdPartidaAsc(idEmpresa);
    }

    public Optional<Partida> findById(Integer id) {
        return partidaRepository.findById(id);
    }

    @Transactional
    public Partida save(Partida partida, List<Movimiento> movimientos, List<DocumentosFuente> documentosFuentes) {
        Partida savedPartida = partidaRepository.save(partida);

        for (Movimiento movimiento : movimientos) {
            movimiento.setIdPartida(savedPartida.getIdPartida());
            movimiento.setIdEmpresa(partida.getIdEmpresa());
            movimiento.setIdUsuarioEmpresa(partida.getIdUsuarioEmpresa());
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

    //    Eliminar partida
    @Transactional
    public void deleteById(Integer id) {
        Partida partida = partidaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partida no encontrada"));
        movimientoRepository.deleteByIdPartida(partida.getIdPartida());
        partidaRepository.delete(partida);
    }

    /**
     * Actualizar una partida existente con sus movimientos y documentos
     */
    @Transactional
    public Partida update(Partida partida, List<Movimiento> movimientos, List<DocumentosFuente> nuevosDocumentos) {
        // Actualizar datos básicos de la partida
        Partida updatedPartida = partidaRepository.save(partida);

        // Eliminar movimientos antiguos
        movimientoRepository.deleteByIdPartida(partida.getIdPartida());

        // Guardar nuevos movimientos
        for (Movimiento movimiento : movimientos) {
            movimiento.setIdPartida(updatedPartida.getIdPartida());
            movimiento.setIdEmpresa(partida.getIdEmpresa());
            movimiento.setIdUsuarioEmpresa(partida.getIdUsuarioEmpresa());
            movimientoRepository.save(movimiento);
        }

        // Guardar nuevos documentos si existen
        if (nuevosDocumentos != null && !nuevosDocumentos.isEmpty()) {
            documentosFuenteRepository.saveAll(nuevosDocumentos);

            for (DocumentosFuente documento : nuevosDocumentos) {
                DocumentosPartida documentosPartida = new DocumentosPartida();
                documentosPartida.setDocumento(documento);
                documentosPartida.setPartida(updatedPartida);
                documentosPartidaRepository.save(documentosPartida);
            }
        }

        return updatedPartida;
    }
}
