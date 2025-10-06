package com.example.LoginCliente.Models;

public class PartidaDTO {
    private Integer id_partida;
    private String concepto;
    private String fechaFormateada;
    private String autor;

    public PartidaDTO(Integer id_partida, String concepto, String fechaFormateada, String autor) {
        this.id_partida = id_partida;
        this.concepto = concepto;
        this.fechaFormateada = fechaFormateada;
        this.autor = autor;
    }

    public Integer getId_partida() {
        return id_partida;
    }

    public void setId_partida(Integer id_partida) {
        this.id_partida = id_partida;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public String getFechaFormateada() {
        return fechaFormateada;
    }

    public void setFechaFormateada(String fechaFormateada) {
        this.fechaFormateada = fechaFormateada;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }
}
