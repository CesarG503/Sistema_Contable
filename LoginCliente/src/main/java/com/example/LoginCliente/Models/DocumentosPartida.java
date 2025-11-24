package com.example.LoginCliente.Models;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "tbl_documentos_partida")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DocumentosPartida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_documento_partida;

    @ManyToOne
    @JoinColumn(name = "id_documento", nullable = false)
    private DocumentosFuente documento;

    @ManyToOne
    @JoinColumn(name = "id_partida", nullable = false)
    private Partida partida;

    @ManyToOne
    @JoinColumn(name="id_empresa", nullable = false)
    private Empresa empresa;
}
