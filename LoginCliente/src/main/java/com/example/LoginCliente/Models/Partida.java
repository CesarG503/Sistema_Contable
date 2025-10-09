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
    private Integer id_partida;

    @Column(nullable = false)
    private Integer autor;

    @Column
    private String concepto;

    @Column
    private Timestamp fecha;
}

