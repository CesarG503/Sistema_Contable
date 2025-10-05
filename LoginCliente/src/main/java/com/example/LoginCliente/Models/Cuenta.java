package com.example.LoginCliente.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "tbl_cuentas")
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

    public Cuenta() {
    }

    public Integer getId_cuenta() {
        return id_cuenta;
    }

    public void setId_cuenta(Integer id_cuenta) {
        this.id_cuenta = id_cuenta;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getNaturaleza() {
        return naturaleza;
    }

    public void setNaturaleza(String naturaleza) {
        this.naturaleza = naturaleza;
    }
}

