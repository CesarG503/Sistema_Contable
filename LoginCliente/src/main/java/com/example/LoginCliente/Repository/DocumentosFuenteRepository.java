package com.example.LoginCliente.Repository;

import com.example.LoginCliente.Models.DocumentosFuente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentosFuenteRepository extends JpaRepository<DocumentosFuente, Integer> {
    @Query("SELECT df FROM DocumentosFuente df JOIN df.partidaDocumentos p WHERE p.partida.idPartida = :idPartida")
    List<DocumentosFuente> findDocumentosByPartidaIdpartida(Integer idPartida);

    @Query("SELECT dp FROM DocumentosFuente dp JOIN dp.partidaDocumentos p WHERE p.partida.idEmpresa = :idEmpresa")
    List<DocumentosFuente> findDocumentosByEmpresaId(Integer idEmpresa);
}
