package com.example.LoginCliente.Models;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "tbl_partidas")
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

    public Partida() {
    }

    public Integer getId_partida() {
        return id_partida;
    }

    public void setId_partida(Integer id_partida) {
        this.id_partida = id_partida;
    }

    public Integer getAutor() {
        return autor;
    }

    public void setAutor(Integer autor) {
        this.autor = autor;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public Timestamp getFecha() {
        return fecha;
    }

    public void setFecha(Timestamp fecha) {
        this.fecha = fecha;
    }
}

