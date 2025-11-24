package com.example.LoginCliente.Models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tbl_documentos_fuente")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DocumentosFuente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_documento;

    @Column
    private String nombre;

    @Column(nullable = false)
    private Timestamp fecha_subida;

    @Column(nullable = false)
    private String ruta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "añadido_por") // columna en la base de datos (sin caracteres no-ASCII)
    private Usuario anadidoPor; // Usuario que añadió el documento (renombrado)

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentosPartida> partidaDocumentos = new ArrayList<>();

    // getters / setters mínimos relevantes
    public Integer getId_documento() {
        return id_documento;
    }

    public void setId_documento(Integer id_documento) {
        this.id_documento = id_documento;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public Timestamp getFecha_subida() {
        return fecha_subida;
    }

    public void setFecha_subida(Timestamp fecha_subida) {
        this.fecha_subida = fecha_subida;
    }

    public Usuario getAnadidoPor() {
        return anadidoPor;
    }

    public void setAnadidoPor(Usuario anadidoPor) {
        this.anadidoPor = anadidoPor;
    }

    public String getTipo(){
        String[] partes = ruta.split("\\.");
        return partes.length > 1 ? partes[partes.length - 1] : "";
    }
}
