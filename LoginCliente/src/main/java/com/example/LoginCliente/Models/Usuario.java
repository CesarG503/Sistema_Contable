package com.example.LoginCliente.Models;

import jakarta.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "tbl_usuarios")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @NotBlank(message = "Usuario es obligatorio")
    @Column(nullable = false, unique = true)
    private String usuario;

    @NotBlank(message = "Correo es obligatorio")
    @Column(nullable = false, unique = true)
    private  String correo;

    @NotBlank(message = "Contraseña es obligatoria")
    @Size(min = 8, message = "Contraseña debe tener al menos 8 caracteres")
    @Column(nullable = false)
    private String pwd;

}
