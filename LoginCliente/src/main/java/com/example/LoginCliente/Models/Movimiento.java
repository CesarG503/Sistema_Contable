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
    @Column(name = "id_movimiento")
    private Integer idMovimiento;

    @Column(nullable = false, name = "id_partida")
    private Integer idPartida;

    @Column(nullable = false, name = "id_cuenta")
    private Integer idCuenta;

    @Column(precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(length = 1)
    private String tipo; // 'D' para Debe, 'H' para Haber

    @Column(name = "id_empresa")
    private Integer idEmpresa;

    @Column(name = "id_usuario_empresa")
    private Integer idUsuarioEmpresa;
}
