package com.example.LoginCliente.Models;

import com.example.LoginCliente.Repository.EmpresaRepository;
import com.example.LoginCliente.Service.EmpresaService;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "tbl_reportes")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Integer idReporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa")
    private Empresa empresa;

    @Column(name = "fecha_inicio")
    private Timestamp fechaInicio;

    @Column(name = "fecha_fin")
    private Timestamp fechaFin;

    @Column(name = "fecha_generacion")
    private Timestamp fechaGeneracion;

    @Column(name = "total_activos")
    private BigDecimal totalActivos;

    @Column(name = "total_pasivos")
    private BigDecimal totalPasivos;

    @Column(name = "total_capital")
    private BigDecimal totalCapital;

    @Column(name = "utilidad_neta")
    private BigDecimal utilidadNeta;

    @Column(name = "datos_json", columnDefinition = "TEXT")
    private String datosJson;

    @ManyToOne
    @JoinColumn(name = "generado_por")
    private Usuario generadoPor;

    // Constructor, getters y setters
    public Reporte() {}

    // Getters y Setters completos
    public Integer getIdReporte() { return idReporte; }
    public void setIdReporte(Integer idReporte) { this.idReporte = idReporte; }

    public Integer getIdEmpresa() { return empresa.getIdEmpresa(); }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa Empresa) {
        this.empresa = Empresa;
    }

    public Timestamp getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Timestamp fechaInicio) { this.fechaInicio = fechaInicio; }

    public Timestamp getFechaFin() { return fechaFin; }
    public void setFechaFin(Timestamp fechaFin) { this.fechaFin = fechaFin; }

    public Timestamp getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(Timestamp fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }

    public BigDecimal getTotalActivos() { return totalActivos; }
    public void setTotalActivos(BigDecimal totalActivos) { this.totalActivos = totalActivos; }

    public BigDecimal getTotalPasivos() { return totalPasivos; }
    public void setTotalPasivos(BigDecimal totalPasivos) { this.totalPasivos = totalPasivos; }

    public BigDecimal getTotalCapital() { return totalCapital; }
    public void setTotalCapital(BigDecimal totalCapital) { this.totalCapital = totalCapital; }

    public BigDecimal getUtilidadNeta() { return utilidadNeta; }
    public void setUtilidadNeta(BigDecimal utilidadNeta) { this.utilidadNeta = utilidadNeta; }

    public String getDatosJson() { return datosJson; }
    public void setDatosJson(String datosJson) { this.datosJson = datosJson; }

    public Usuario getGeneradoPor() { return generadoPor; }
    public void setGeneradoPor(Usuario generadoPor) { this.generadoPor = generadoPor; }
}