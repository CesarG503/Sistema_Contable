package com.example.LoginCliente.Models;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tbl_movimientos")
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_movimiento;

    @Column(nullable = false, name = "id_partida")
    private Integer idPartida;

    @Column(nullable = false)
    private Integer id_cuenta;

    @Column(precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(length = 1)
    private String tipo; // 'D' para Debe, 'H' para Haber

    public Movimiento() {
    }

    public Integer getId_movimiento() {
        return id_movimiento;
    }

    public void setId_movimiento(Integer id_movimiento) {
        this.id_movimiento = id_movimiento;
    }

    public Integer getIdPartida() {
        return idPartida;
    }

    public void setIdPartida(Integer idPartida) {
        this.idPartida = idPartida;
    }

    public Integer getId_cuenta() {
        return id_cuenta;
    }

    public void setId_cuenta(Integer id_cuenta) {
        this.id_cuenta = id_cuenta;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

   public void setId_partida(Integer idPartida) {
       this.idPartida = idPartida;
   }
}
