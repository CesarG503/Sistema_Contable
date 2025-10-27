package com.example.LoginCliente.Service;

import com.example.LoginCliente.Models.DocumentosFuente;
import com.example.LoginCliente.Repository.DocumentosFuenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentosPartidaService {
    @Autowired
    private DocumentosFuenteRepository documentosFuenteRepository;

    public List<DocumentosFuente> findDocumentosByPartidaId(Integer idPartida) {
        return documentosFuenteRepository.findDocumentosByPartidaIdpartida(idPartida);
    }
}
