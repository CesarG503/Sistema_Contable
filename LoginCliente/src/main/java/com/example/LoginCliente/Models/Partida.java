package com.example.LoginCliente.Models;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tbl_partidas")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_partida")
    private Integer idPartida;

    @Column(nullable = false)
    private Integer autor;

    @Column
    private String concepto;

    @Column
    private Timestamp fecha;

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentosPartida> partidaDocumentos = new ArrayList<>();
    
    @Column(nullable = false, name = "id_empresa")
    private Integer idEmpresa;

    @Column(name = "id_usuario_empresa")
    private Integer idUsuarioEmpresa;
}
