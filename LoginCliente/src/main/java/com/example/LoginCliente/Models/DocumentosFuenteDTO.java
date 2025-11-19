package com.example.LoginCliente.Models;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DocumentosFuenteDTO {
    private Integer id;
    private String nombre;
    private String ruta;
    private String fechaSubida;
    private String anadidoPor; // renamed to avoid non-ASCII

    // Constructor explícito para evitar depender únicamente de Lombok y asegurar firma
    public DocumentosFuenteDTO(Integer id, String nombre, String ruta, String fechaSubida, String anadidoPor) {
        this.id = id;
        this.nombre = nombre;
        this.ruta = ruta;
        this.fechaSubida = fechaSubida;
        this.anadidoPor = anadidoPor;
    }
}
