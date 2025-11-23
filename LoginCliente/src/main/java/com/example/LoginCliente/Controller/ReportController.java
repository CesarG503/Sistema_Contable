package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Models.*;
import com.example.LoginCliente.Service.CuentaService;
import com.example.LoginCliente.Service.PartidaService;
import com.example.LoginCliente.Service.UsuarioService;
import com.example.LoginCliente.Service.DocumentosPartidaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/reportes")
public class ReportController {

    @Autowired
    private PartidaService partidaService;

    @Autowired
    private CuentaService cuentaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private DocumentosPartidaService documentosPartidaService;

    @GetMapping("/generar")
    public String generarReporte(Model model, HttpSession session) {
        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
        if (empresaActiva == null) {
            return "redirect:/empresas/mis-empresas";
        }

        List<Cuenta> cuentas = cuentaService.findByIdEmpresa(empresaActiva);
        model.addAttribute("cuentas", cuentas);
        model.addAttribute("page", "generar-reporte");

        return "generar-reporte";
    }

    @PostMapping("/obtener-datos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerDatosReporte(
            @RequestParam("fechaInicio") String fechaInicio,
            @RequestParam("fechaFin") String fechaFin,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        try {
            Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
            if (empresaActiva == null) {
                response.put("error", "No hay empresa seleccionada");
                return ResponseEntity.badRequest().body(response);
            }

            // Convertir fechas
            LocalDate inicio = LocalDate.parse(fechaInicio);
            LocalDate fin = LocalDate.parse(fechaFin);
            Timestamp tsInicio = Timestamp.valueOf(inicio.atStartOfDay());
            Timestamp tsFin = Timestamp.valueOf(fin.atTime(23, 59, 59));

            // Obtener partidas en el rango de fechas
            List<Partida> partidas = partidaService.findByIdEmpresaAndDateRange(empresaActiva, tsInicio, tsFin);
            List<Cuenta> cuentas = cuentaService.findByIdEmpresa(empresaActiva);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // ===== LIBRO DIARIO (PARTIDA DOBLE) =====
            List<Map<String, Object>> libroDiario = new ArrayList<>();
            Map<Integer, List<DocumentosFuenteDTO>> documentosPorPartida = new HashMap<>();

            for (Partida partida : partidas) {
                String fechaFormateada = "";
                if (partida.getFecha() != null) {
                    if (partida.getFecha() instanceof Timestamp) {
                        fechaFormateada = ((Timestamp) partida.getFecha()).toLocalDateTime().format(formatter);
                    }
                }

                Map<String, Object> partidaData = new HashMap<>();
                partidaData.put("idPartida", partida.getIdPartida());
                partidaData.put("fecha", fechaFormateada);
                partidaData.put("concepto", partida.getConcepto());

                List<Movimiento> movimientos = partidaService.findMovimientosByPartida(partida.getIdPartida());
                BigDecimal totalDebe = BigDecimal.ZERO;
                BigDecimal totalHaber = BigDecimal.ZERO;

                List<Map<String, Object>> movimientosData = new ArrayList<>();
                for (Movimiento mov : movimientos) {
                    String nombreCuenta = "";
                    for (Cuenta cuenta : cuentas) {
                        if (cuenta.getIdCuenta().equals(mov.getIdCuenta())) {
                            nombreCuenta = cuenta.getNombre();
                            break;
                        }
                    }

                    Map<String, Object> movData = new HashMap<>();
                    movData.put("nombreCuenta", nombreCuenta);
                    movData.put("monto", mov.getMonto());
                    movData.put("tipo", mov.getTipo());

                    if ("D".equals(mov.getTipo())) {
                        totalDebe = totalDebe.add(mov.getMonto());
                    } else {
                        totalHaber = totalHaber.add(mov.getMonto());
                    }

                    movimientosData.add(movData);
                }

                partidaData.put("movimientos", movimientosData);
                partidaData.put("totalDebe", totalDebe);
                partidaData.put("totalHaber", totalHaber);
                libroDiario.add(partidaData);

                // Documentos de la partida
                List<DocumentosFuente> documentos = documentosPartidaService.findDocumentosByPartidaId(partida.getIdPartida());
                List<DocumentosFuenteDTO> documentosDTO = new ArrayList<>();
                documentos.forEach(documento -> {
                    String authorUsuario = documento.getAnadidoPor() != null ? documento.getAnadidoPor().getUsuario() : "";
                    documentosDTO.add(new DocumentosFuenteDTO(
                            documento.getId_documento(),
                            documento.getNombre(),
                            documento.getRuta(),
                            documento.getFecha_subida() != null ? documento.getFecha_subida().toString() : "",
                            authorUsuario
                    ));
                });
                documentosPorPartida.put(partida.getIdPartida(), documentosDTO);
            }

            // ===== LIBRO MAYOR (CUENTAS T) =====
            Map<String, Map<String, Object>> libroMayor = new LinkedHashMap<>();
            for (Cuenta cuenta : cuentas) {
                Map<String, Object> cuentaData = new HashMap<>();
                cuentaData.put("nombre", cuenta.getNombre());
                cuentaData.put("tipo", cuenta.getTipo());
                cuentaData.put("naturaleza", cuenta.getNaturaleza());
                cuentaData.put("descripcion", cuenta.getDescripcion());

                List<Movimiento> movimientos = cuentaService.obtenerMovimientosPorCuentaYFechas(cuenta.getIdCuenta(), tsInicio, tsFin);

                BigDecimal totalDebe = BigDecimal.ZERO;
                BigDecimal totalHaber = BigDecimal.ZERO;
                List<Map<String, Object>> detalles = new ArrayList<>();

                for (Movimiento mov : movimientos) {
                    Map<String, Object> detalle = new HashMap<>();
                    detalle.put("monto", mov.getMonto());
                    detalle.put("tipo", mov.getTipo());
                    detalle.put("idPartida", mov.getIdPartida());
                    detalles.add(detalle);

                    if ("D".equals(mov.getTipo())) {
                        totalDebe = totalDebe.add(mov.getMonto());
                    } else {
                        totalHaber = totalHaber.add(mov.getMonto());
                    }
                }

                BigDecimal saldo;
                if ("D".equals(cuenta.getNaturaleza())) {
                    saldo = totalDebe.subtract(totalHaber);
                } else {
                    saldo = totalHaber.subtract(totalDebe);
                }

                cuentaData.put("totalDebe", totalDebe);
                cuentaData.put("totalHaber", totalHaber);
                cuentaData.put("saldo", saldo);
                cuentaData.put("detalles", detalles);

                libroMayor.put(cuenta.getNombre(), cuentaData);
            }

            // ===== BALANCE DE COMPROBACIÓN =====
            List<Map<String, Object>> balanceComprobacion = new ArrayList<>();
            BigDecimal totalDebeBC = BigDecimal.ZERO;
            BigDecimal totalHaberBC = BigDecimal.ZERO;

            for (Cuenta cuenta : cuentas) {
                List<Movimiento> movimientos = cuentaService.obtenerMovimientosPorCuentaYFechas(cuenta.getIdCuenta(), tsInicio, tsFin);

                BigDecimal totalDebe = BigDecimal.ZERO;
                BigDecimal totalHaber = BigDecimal.ZERO;

                for (Movimiento mov : movimientos) {
                    if ("D".equals(mov.getTipo())) {
                        totalDebe = totalDebe.add(mov.getMonto());
                    } else {
                        totalHaber = totalHaber.add(mov.getMonto());
                    }
                }

                if (totalDebe.compareTo(BigDecimal.ZERO) > 0 || totalHaber.compareTo(BigDecimal.ZERO) > 0) {
                    Map<String, Object> cuentaBC = new HashMap<>();
                    cuentaBC.put("nombre", cuenta.getNombre());
                    cuentaBC.put("tipo", cuenta.getTipo());
                    cuentaBC.put("debe", totalDebe);
                    cuentaBC.put("haber", totalHaber);
                    balanceComprobacion.add(cuentaBC);

                    totalDebeBC = totalDebeBC.add(totalDebe);
                    totalHaberBC = totalHaberBC.add(totalHaber);
                }
            }

            Map<String, Object> totalesBC = new HashMap<>();
            totalesBC.put("debe", totalDebeBC);
            totalesBC.put("haber", totalHaberBC);
            response.put("balanceComprobacion", balanceComprobacion);
            response.put("totalesBalanceComprobacion", totalesBC);

            // ===== BALANCE GENERAL =====
            Map<String, BigDecimal> ecuacionContable = cuentaService.calcularEcuacionContableParaPeriodo(empresaActiva, tsInicio, tsFin);

            List<Map<String, Object>> activos = new ArrayList<>();
            List<Map<String, Object>> pasivos = new ArrayList<>();
            List<Map<String, Object>> capital = new ArrayList<>();
            List<Map<String, Object>> ingresos = new ArrayList<>();
            List<Map<String, Object>> gastos = new ArrayList<>();
            List<Map<String, Object>> retiros = new ArrayList<>();
            List<Map<String, Object>> capitalAccounts = new ArrayList<>();

            BigDecimal totalIngresos = BigDecimal.ZERO;
            BigDecimal totalGastos = BigDecimal.ZERO;
            BigDecimal totalRetiros = BigDecimal.ZERO;
            BigDecimal totalCapitalInicial = BigDecimal.ZERO;

            for (Cuenta cuenta : cuentas) {
                String tipo = cuenta.getTipo();
                if (tipo == null) continue;

                List<Movimiento> movimientos = cuentaService.obtenerMovimientosPorCuentaYFechas(cuenta.getIdCuenta(), tsInicio, tsFin);
                BigDecimal saldo = BigDecimal.ZERO;

                BigDecimal totalDebeCuenta = BigDecimal.ZERO;
                BigDecimal totalHaberCuenta = BigDecimal.ZERO;

                for (Movimiento mov : movimientos) {
                    if ("D".equals(mov.getTipo())) {
                        saldo = saldo.add(mov.getMonto());
                        totalDebeCuenta = totalDebeCuenta.add(mov.getMonto());
                    } else {
                        saldo = saldo.subtract(mov.getMonto());
                        totalHaberCuenta = totalHaberCuenta.add(mov.getMonto());
                    }
                }

                // Determine nature-based balance for display
                BigDecimal saldoNaturaleza;
                if ("D".equals(cuenta.getNaturaleza())) {
                    saldoNaturaleza = saldo; // Debit is positive
                } else {
                    saldoNaturaleza = saldo.negate(); // Credit is positive
                }

                // Only add if there's a balance or movement
                if (saldoNaturaleza.compareTo(BigDecimal.ZERO) != 0 || totalDebeCuenta.compareTo(BigDecimal.ZERO) != 0 || totalHaberCuenta.compareTo(BigDecimal.ZERO) != 0) {
                    Map<String, Object> cuentaData = new HashMap<>();
                    cuentaData.put("nombre", cuenta.getNombre());
                    cuentaData.put("saldo", saldoNaturaleza);
                    cuentaData.put("numeroCuenta", cuenta.getNumeroCuenta());

                    if ("ACTIVO".equalsIgnoreCase(tipo)) {
                        activos.add(cuentaData);
                    } else if ("PASIVO".equalsIgnoreCase(tipo)) {
                        pasivos.add(cuentaData);
                    } else if ("CAPITAL".equalsIgnoreCase(tipo)) {
                        capital.add(cuentaData);
                        // Logic for Statement of Owner's Equity
                        // Assuming 'D' nature in Capital implies Withdrawals (Retiros)
                        if ("D".equals(cuenta.getNaturaleza())) {
                            retiros.add(cuentaData);
                            totalRetiros = totalRetiros.add(saldoNaturaleza);
                        } else {
                            capitalAccounts.add(cuentaData);
                            totalCapitalInicial = totalCapitalInicial.add(saldoNaturaleza);
                        }
                    } else if ("INGRESO".equalsIgnoreCase(tipo)) {
                        ingresos.add(cuentaData);
                        totalIngresos = totalIngresos.add(saldoNaturaleza);
                    } else if ("GASTO".equalsIgnoreCase(tipo)) {
                        gastos.add(cuentaData);
                        totalGastos = totalGastos.add(saldoNaturaleza);
                    }
                }
            }

            // Calculate Net Income
            BigDecimal utilidadNeta = totalIngresos.subtract(totalGastos);

            // Calculate Final Capital
            // Capital Final = Capital Inicial + Utilidad Neta - Retiros
            BigDecimal capitalFinal = totalCapitalInicial.add(utilidadNeta).subtract(totalRetiros);

            response.put("libroDiario", libroDiario);
            response.put("documentosPorPartida", documentosPorPartida);
            response.put("libroMayor", libroMayor);
            response.put("activos", activos);
            response.put("pasivos", pasivos);
            response.put("capital", capital);

            response.put("ingresos", ingresos);
            response.put("gastos", gastos);
            response.put("totalIngresos", totalIngresos);
            response.put("totalGastos", totalGastos);
            response.put("utilidadNeta", utilidadNeta);

            response.put("capitalAccounts", capitalAccounts); // Only credit nature capital accounts
            response.put("retiros", retiros);
            response.put("totalRetiros", totalRetiros);
            response.put("totalCapitalInicial", totalCapitalInicial);
            response.put("capitalFinal", capitalFinal);

            response.put("ecuacionContable", ecuacionContable);
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error al obtener datos del reporte: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }
}
