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
    private Integer id_usuario;

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

    @Column
    private Integer permiso;

    public Permiso getPermiso() {
        return Permiso.valueOfValor(permiso);
    }

    public void setPermiso( Permiso permiso) {
        this.permiso =  permiso.valor;
    }
}

