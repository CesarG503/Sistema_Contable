package com.example.LoginCliente.Models;

import lombok.*;

@Getter
@Setter
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
}
