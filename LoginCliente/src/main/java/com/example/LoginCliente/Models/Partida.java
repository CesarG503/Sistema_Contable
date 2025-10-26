package com.example.LoginCliente.Models;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

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

    @Column(nullable = false, name = "id_empresa")
    private Integer idEmpresa;

    @Column(name = "id_usuario_empresa")
    private Integer idUsuarioEmpresa;
}
