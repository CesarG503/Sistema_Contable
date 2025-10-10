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
    private Integer idPartida;

    @Column(nullable = false)
    private Integer autor;

    @Column
    private String concepto;

    @Column
    private Timestamp fecha;

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentosPartida> partidaDocumentos = new ArrayList<>();


    public Integer getIdPartida() {
        return this.idPartida;
    }

    public void setIdPartida(Integer idPartida) {
        this.idPartida = idPartida;
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

    public Integer getId_partida() {
        return idPartida;
    }
}
