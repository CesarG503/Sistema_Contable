package com.example.LoginCliente.Models;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "tbl_tipo_documento")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TipoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_tipo;

    @Column(nullable = false)
    private String nombre;
}

