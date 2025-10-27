package com.example.LoginCliente.Models;

import lombok.*;

import java.util.List;

@Getter
@Setter
public class PartidaDTO {
    private Integer idPartida;
    private String concepto;
    private String fechaFormateada;
    private List<DocumentosFuenteDTO> documentos;
    private String autor;

    public PartidaDTO(Integer idPartida, String concepto, String fechaFormateada, String autor) {
        this.idPartida = idPartida;
        this.concepto = concepto;
        this.fechaFormateada = fechaFormateada;
        this.autor = autor;
    }

    public PartidaDTO(Integer idPartida, String concepto, String fechaFormateada, List<DocumentosFuenteDTO> documentos, String autor) {
        this.idPartida = idPartida;
        this.concepto = concepto;
        this.fechaFormateada = fechaFormateada;
        this.documentos = documentos;
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

    public List<DocumentosFuenteDTO> getDocumentos() {
        return documentos;
    }

    public void setDocumentos(List<DocumentosFuenteDTO> documentos) {
        this.documentos = documentos;
    }
}
