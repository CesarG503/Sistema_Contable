package com.example.LoginCliente.Models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tbl_documentos_fuente")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DocumentosFuente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_documento;

    @Column
    private String nombre;

    @Column(nullable = false)
    private Timestamp fecha_subida;

    @Column(nullable = false)
    private String ruta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "añadido_por") // columna en la base de datos
    private Usuario añadidoPor; // Usuario que añadió el documento

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentosPartida> partidaDocumentos = new ArrayList<>();
}
