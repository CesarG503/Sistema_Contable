package com.example.LoginCliente.Models;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "tbl_cuentas")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_cuenta;

    @Column(nullable = false)
    private String nombre;

    @Column
    private String descripcion;

    @Column(length = 1)
    private String naturaleza; // 'D' para Deudora, 'A' para Acreedora
}

