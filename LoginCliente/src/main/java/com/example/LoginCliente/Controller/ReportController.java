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

                // Calculate net balance: Debe - Haber
                BigDecimal saldoNeto = totalDebe.subtract(totalHaber);
                BigDecimal debeFinal = BigDecimal.ZERO;
                BigDecimal haberFinal = BigDecimal.ZERO;

                // If saldoNeto is positive, it's a Debit balance. If negative, it's a Credit balance.
                if (saldoNeto.compareTo(BigDecimal.ZERO) > 0) {
                    debeFinal = saldoNeto;
                } else if (saldoNeto.compareTo(BigDecimal.ZERO) < 0) {
                    haberFinal = saldoNeto.abs();
                }

                // Only add if there is a non-zero balance or there was activity (optional, but cleaner if we only show active accounts)
                if (totalDebe.compareTo(BigDecimal.ZERO) > 0 || totalHaber.compareTo(BigDecimal.ZERO) > 0) {
                    Map<String, Object> cuentaBC = new HashMap<>();
                    cuentaBC.put("nombre", cuenta.getNombre());
                    cuentaBC.put("tipo", cuenta.getTipo());
                    cuentaBC.put("debe", debeFinal);
                    cuentaBC.put("haber", haberFinal);
                    balanceComprobacion.add(cuentaBC);

                    totalDebeBC = totalDebeBC.add(debeFinal);
                    totalHaberBC = totalHaberBC.add(haberFinal);
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

            // ===== ESTADO DE FLUJO DE EFECTIVO (MÉTODO DIRECTO) =====
            Map<String, Object> flujoEfectivo = new HashMap<>();

            // 1. Identificar cuentas de Efectivo/Equivalentes
            List<Integer> idsCuentasEfectivo = new ArrayList<>();
            List<Cuenta> cuentasEfectivo = new ArrayList<>();

            for (Cuenta c : cuentas) {
                String nombreLower = c.getNombre() != null ? c.getNombre().toLowerCase() : "";
                String tipo = c.getTipo() != null ? c.getTipo().toUpperCase() : "";
                String numeroCuenta = c.getNumeroCuenta() != null ? c.getNumeroCuenta() : "";

                // 1. Por tipo (ACTIVO) y palabras clave ampliadas
                // 2. Por código contable estándar (comienza con 101, 102, 11, 1.1)
                boolean esEfectivoNombre = nombreLower.contains("caja") ||
                        nombreLower.contains("banco") ||
                        nombreLower.contains("efectivo") ||
                        nombreLower.contains("disponible") ||
                        nombreLower.contains("tesoreria") ||
                        nombreLower.contains("cash");

                boolean esEfectivoCodigo = numeroCuenta.startsWith("101") ||
                        numeroCuenta.startsWith("102") ||
                        numeroCuenta.startsWith("1101") ||
                        numeroCuenta.startsWith("1102") ||
                        numeroCuenta.startsWith("1.1.01") ||
                        numeroCuenta.startsWith("1.1.02");

                if (tipo.contains("ACTIVO") && (esEfectivoNombre || esEfectivoCodigo)) {
                    idsCuentasEfectivo.add(c.getIdCuenta());
                    cuentasEfectivo.add(c);
                }
            }

            // 2. Calcular Saldo Inicial de Efectivo (antes del rango)
            BigDecimal saldoInicialEfectivo = BigDecimal.ZERO;
            for (Integer idCuenta : idsCuentasEfectivo) {
                // Obtener movimientos previos a fechaInicio
                // Nota: Esto requería un servicio específico o lógica adicional.
                // Por simplificación, usaremos el saldo calculado de movimientos totales - movimientos del periodo
                // O mejor, consultamos el saldo total hasta fechaInicio

                // Opción robusta: Calcular saldo acumulado hasta el día anterior al inicio
                List<Movimiento> movsPrevios = cuentaService.obtenerMovimientosPorCuentaYFechas(idCuenta, Timestamp.valueOf("1900-01-01 00:00:00"), Timestamp.valueOf(tsInicio.toLocalDateTime().minusSeconds(1)));
                for(Movimiento m : movsPrevios) {
                    if ("D".equals(m.getTipo())) {
                        saldoInicialEfectivo = saldoInicialEfectivo.add(m.getMonto());
                    } else {
                        saldoInicialEfectivo = saldoInicialEfectivo.subtract(m.getMonto());
                    }
                }
            }

            // 3. Clasificar movimientos del periodo
            List<Map<String, Object>> actividadesOperacion = new ArrayList<>();
            List<Map<String, Object>> actividadesInversion = new ArrayList<>();
            List<Map<String, Object>> actividadesFinanciamiento = new ArrayList<>();

            BigDecimal totalOperacion = BigDecimal.ZERO;
            BigDecimal totalInversion = BigDecimal.ZERO;
            BigDecimal totalFinanciamiento = BigDecimal.ZERO;

            // Iteramos sobre las partidas del periodo
            for (Partida partida : partidas) {
                List<Movimiento> movsPartida = partidaService.findMovimientosByPartida(partida.getIdPartida());

                // Verificar si esta partida afecta efectivo
                boolean afectaEfectivo = false;
                BigDecimal montoEfectivoNeto = BigDecimal.ZERO; // Positivo = Entrada, Negativo = Salida

                for (Movimiento m : movsPartida) {
                    if (idsCuentasEfectivo.contains(m.getIdCuenta())) {
                        afectaEfectivo = true;
                        if ("D".equals(m.getTipo())) {
                            montoEfectivoNeto = montoEfectivoNeto.add(m.getMonto());
                        } else {
                            montoEfectivoNeto = montoEfectivoNeto.subtract(m.getMonto());
                        }
                    }
                }

                if (afectaEfectivo && montoEfectivoNeto.compareTo(BigDecimal.ZERO) != 0) {
                    // Buscar la contrapartida (causa del flujo)
                    // Simplificación: Buscamos la cuenta principal que NO es efectivo con mayor monto
                    String conceptoFlujo = partida.getConcepto();
                    String tipoActividad = "OPERACION"; // Default

                    // Análisis básico de contrapartidas para clasificar
                    for (Movimiento m : movsPartida) {
                        if (!idsCuentasEfectivo.contains(m.getIdCuenta())) {
                            // Buscar la cuenta
                            Cuenta cContra = null;
                            for(Cuenta c : cuentas) {
                                if(c.getIdCuenta().equals(m.getIdCuenta())) {
                                    cContra = c;
                                    break;
                                }
                            }

                            if (cContra != null) {
                                String nombreContra = cContra.getNombre().toLowerCase();
                                String tipoContra = cContra.getTipo();

                                // Lógica de clasificación heurística
                                if (nombreContra.contains("capital") || nombreContra.contains("socio") || nombreContra.contains("prestamo") || nombreContra.contains("hipoteca")) {
                                    tipoActividad = "FINANCIAMIENTO";
                                } else if (nombreContra.contains("equipo") || nombreContra.contains("maquinaria") || nombreContra.contains("edificio") || nombreContra.contains("terreno") || nombreContra.contains("vehiculo") || nombreContra.contains("mobiliario")) {
                                    tipoActividad = "INVERSION";
                                }
                                // Si no cae en los anteriores, se queda en OPERACION (Ventas, Gastos, Clientes, Proveedores)
                            }
                        }
                    }

                    Map<String, Object> flujoItem = new HashMap<>();
                    flujoItem.put("concepto", conceptoFlujo);
                    flujoItem.put("monto", montoEfectivoNeto); // Positivo entrada, negativo salida

                    if ("OPERACION".equals(tipoActividad)) {
                        actividadesOperacion.add(flujoItem);
                        totalOperacion = totalOperacion.add(montoEfectivoNeto);
                    } else if ("INVERSION".equals(tipoActividad)) {
                        actividadesInversion.add(flujoItem);
                        totalInversion = totalInversion.add(montoEfectivoNeto);
                    } else {
                        actividadesFinanciamiento.add(flujoItem);
                        totalFinanciamiento = totalFinanciamiento.add(montoEfectivoNeto);
                    }
                }
            }

            BigDecimal flujoNetoTotal = totalOperacion.add(totalInversion).add(totalFinanciamiento);
            BigDecimal saldoFinalEfectivoCalculado = saldoInicialEfectivo.add(flujoNetoTotal);

            flujoEfectivo.put("saldoInicial", saldoInicialEfectivo);
            flujoEfectivo.put("saldoFinal", saldoFinalEfectivoCalculado);
            flujoEfectivo.put("flujoNetoTotal", flujoNetoTotal);

            flujoEfectivo.put("actividadesOperacion", actividadesOperacion);
            flujoEfectivo.put("totalOperacion", totalOperacion);

            flujoEfectivo.put("actividadesInversion", actividadesInversion);
            flujoEfectivo.put("totalInversion", totalInversion);

            flujoEfectivo.put("actividadesFinanciamiento", actividadesFinanciamiento);
            flujoEfectivo.put("totalFinanciamiento", totalFinanciamiento);

            response.put("flujoEfectivo", flujoEfectivo);

            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error al obtener datos del reporte: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }
}
