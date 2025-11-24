package com.example.LoginCliente.Models;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "tbl_usuarios_empresas")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UsuarioEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario_empresa")
    private Integer idUsuarioEmpresa;

    @Column(nullable = false, name = "id_usuario")
    private Integer idUsuario;

    @Column(nullable = false, name = "id_empresa")
    private Integer idEmpresa;

    @Column
    private Integer permiso;

    @Column(name = "fecha_afiliacion")
    private Timestamp fechaAfiliacion;

    @ManyToOne
    @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "id_empresa", insertable = false, updatable = false)
    private Empresa empresa;

    // Devuelve el enum Permiso asociado al entero almacenado
    public Permiso getPermiso() {
        return Permiso.valueOfValor(permiso);
    }

    public void setPermiso(Permiso permiso) {
        this.permiso = permiso.valor;
    }

    public Integer getPermisoValor() {
        return permiso;
    }

    public String getPermisoTexto() {
        Permiso p = getPermiso();
        return p != null ? p.texto : "";
    }
}
