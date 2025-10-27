package com.example.LoginCliente.Models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class DocumentosFuenteDTO {
    private Integer id;
    private String nombre;
    private String ruta;
    private String fechaSubida;
    private BigDecimal valor;
    private String añadidoPor;
}
