package com.example.LoginCliente.Models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "tbl_movimientos")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_movimiento;

    @Column(nullable = false, name = "id_partida")
    private Integer id_partida;

    @Column(nullable = false)
    private Integer id_cuenta;

    @Column(precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(length = 1)
    private String tipo; // 'D' para Debe, 'H' para Haber
}
