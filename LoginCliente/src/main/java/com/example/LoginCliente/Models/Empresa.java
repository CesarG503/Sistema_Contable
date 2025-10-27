package com.example.LoginCliente.Models;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "tbl_empresas")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empresa")
    private Integer idEmpresa;

    @Column(nullable = false)
    private String nombre;

    @Column(unique = true)
    private String nit;

    @Column
    private String direccion;

    @Column
    private String descripcion;

    @Column
    private String telefono;

    @Column(name = "fecha_registro")
    private Timestamp fechaRegistro;
}
