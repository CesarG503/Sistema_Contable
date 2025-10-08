package com.example.LoginCliente.Models;

public class PartidaDTO {
    private Integer idPartida;
    private String concepto;
    private String fechaFormateada;
    private String autor;

    public PartidaDTO(Integer idPartida, String concepto, String fechaFormateada, String autor) {
        this.idPartida = idPartida;
        this.concepto = concepto;
        this.fechaFormateada = fechaFormateada;
        this.autor = autor;
    }

    public Integer getIdPartida() {
        return idPartida;
    }

    public void setIdPartida(Integer idPartida) {
        this.idPartida = idPartida;
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
