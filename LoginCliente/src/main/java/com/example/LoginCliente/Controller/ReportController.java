package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Models.*;
import com.example.LoginCliente.Service.*;
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

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private EmpresaService empresaService;

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
            response.put("fechaInicio", fechaInicio);
            response.put("fechaFin", fechaFin);
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error al obtener datos del reporte: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarReporte(
            @RequestBody Map<String, Object> datosReporte,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        try {
            Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
            if (empresaActiva == null) {
                response.put("error", "No hay empresa seleccionada");
                return ResponseEntity.badRequest().body(response);
            }

            // Obtener usuario actual
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            Usuario usuario = usuarioService.findByUsuario(username);

            // Crear el reporte
            Reporte reporte = new Reporte();
            Empresa empresa = empresaService.findById(empresaActiva).get();
            reporte.setEmpresa(empresa);

            // Parsear fechas
            String fechaInicio = (String) datosReporte.get("fechaInicio");
            String fechaFin = (String) datosReporte.get("fechaFin");
            LocalDate inicio = LocalDate.parse(fechaInicio);
            LocalDate fin = LocalDate.parse(fechaFin);

            reporte.setFechaInicio(Timestamp.valueOf(inicio.atStartOfDay()));
            reporte.setFechaFin(Timestamp.valueOf(fin.atTime(23, 59, 59)));
            reporte.setFechaGeneracion(Timestamp.valueOf(LocalDateTime.now()));
            reporte.setGeneradoPor(usuario);

            // Calcular totales de activos y pasivos desde los datos
            List<Map<String, Object>> activos = (List<Map<String, Object>>) datosReporte.get("activos");
            List<Map<String, Object>> pasivos = (List<Map<String, Object>>) datosReporte.get("pasivos");

            BigDecimal totalActivos = activos != null ?
                    activos.stream()
                            .map(a -> new BigDecimal(a.get("saldo").toString()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

            BigDecimal totalPasivos = pasivos != null ?
                    pasivos.stream()
                            .map(p -> new BigDecimal(p.get("saldo").toString()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

            reporte.setTotalActivos(totalActivos);
            reporte.setTotalPasivos(totalPasivos);
            reporte.setTotalCapital(new BigDecimal(datosReporte.get("capitalFinal").toString()));
            reporte.setUtilidadNeta(new BigDecimal(datosReporte.get("utilidadNeta").toString()));

            // Guardar solo los campos seleccionados en JSON
            Map<String, Object> datosCompletos = new HashMap<>();
            datosCompletos.put("balanceComprobacion", datosReporte.get("balanceComprobacion"));
            datosCompletos.put("totalesBalanceComprobacion", datosReporte.get("totalesBalanceComprobacion"));
            datosCompletos.put("libroDiario", datosReporte.get("libroDiario"));
            datosCompletos.put("documentosPorPartida", datosReporte.get("documentosPorPartida"));
            datosCompletos.put("libroMayor", datosReporte.get("libroMayor"));
            datosCompletos.put("activos", datosReporte.get("activos"));
            datosCompletos.put("pasivos", datosReporte.get("pasivos"));
            datosCompletos.put("capital", datosReporte.get("capital"));
            datosCompletos.put("ingresos", datosReporte.get("ingresos"));
            datosCompletos.put("gastos", datosReporte.get("gastos"));
            datosCompletos.put("totalIngresos", datosReporte.get("totalIngresos"));
            datosCompletos.put("totalGastos", datosReporte.get("totalGastos"));
            datosCompletos.put("utilidadNeta", datosReporte.get("utilidadNeta"));
            datosCompletos.put("capitalAccounts", datosReporte.get("capitalAccounts"));
            datosCompletos.put("retiros", datosReporte.get("retiros"));
            datosCompletos.put("totalRetiros", datosReporte.get("totalRetiros"));
            datosCompletos.put("totalCapitalInicial", datosReporte.get("totalCapitalInicial"));
            datosCompletos.put("capitalFinal", datosReporte.get("capitalFinal"));
            datosCompletos.put("ecuacionContable", datosReporte.get("ecuacionContable"));
            datosCompletos.put("fechaInicio", datosReporte.get("fechaInicio"));
            datosCompletos.put("fechaFin", datosReporte.get("fechaFin"));

            String jsonData = objectMapper.writeValueAsString(datosCompletos);
            reporte.setDatosJson(jsonData);

            // Guardar en la base de datos
            Reporte reporteGuardado = reporteService.save(reporte);

            response.put("success", true);
            response.put("idReporte", reporteGuardado.getIdReporte());
            response.put("mensaje", "Reporte guardado exitosamente");

            return ResponseEntity.ok(response);

        } catch (JsonProcessingException e) {
            response.put("error", "Error al procesar JSON: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("error", "Error al guardar el reporte: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/reportes")
    public String verReportes(Model model, HttpSession session) {
        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
        if (empresaActiva == null) {
            return "redirect:/empresas/mis-empresas";
        }

        List<Reporte> reportes = reporteService.findByIdEmpresa(empresaActiva);
        model.addAttribute("reportes", reportes);
        model.addAttribute("page", "ver-reportes");

        return "lista-reportes";
    }

    @GetMapping("/{idReporte}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerReportePorId(
            @PathVariable Integer idReporte,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        try {
            Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
            if (empresaActiva == null) {
                response.put("error", "No hay empresa seleccionada");
                return ResponseEntity.badRequest().body(response);
            }

            // Buscar el reporte
            Optional<Reporte> reporteOpt = reporteService.findById(idReporte);
            if (!reporteOpt.isPresent()) {
                response.put("error", "Reporte no encontrado");
                return ResponseEntity.notFound().build();
            }

            Reporte reporte = reporteOpt.get();

            // Verificar que el reporte pertenece a la empresa activa
            if (!reporte.getEmpresa().getIdEmpresa().equals(empresaActiva)) {
                response.put("error", "No tiene permisos para acceder a este reporte");
                return ResponseEntity.status(403).body(response);
            }

            // Parsear el JSON almacenado
            Map<String, Object> datosReporte = objectMapper.readValue(
                reporte.getDatosJson(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
            );

            // Agregar los datos parseados a la respuesta
            response.put("libroDiario", datosReporte.get("libroDiario"));
            response.put("documentosPorPartida", datosReporte.get("documentosPorPartida"));
            response.put("libroMayor", datosReporte.get("libroMayor"));
            response.put("balanceComprobacion", datosReporte.get("balanceComprobacion"));
            response.put("totalesBalanceComprobacion", datosReporte.get("totalesBalanceComprobacion"));
            response.put("activos", datosReporte.get("activos"));
            response.put("pasivos", datosReporte.get("pasivos"));
            response.put("capital", datosReporte.get("capital"));
            response.put("ingresos", datosReporte.get("ingresos"));
            response.put("gastos", datosReporte.get("gastos"));
            response.put("totalIngresos", datosReporte.get("totalIngresos"));
            response.put("totalGastos", datosReporte.get("totalGastos"));
            response.put("utilidadNeta", datosReporte.get("utilidadNeta"));
            response.put("capitalAccounts", datosReporte.get("capitalAccounts"));
            response.put("retiros", datosReporte.get("retiros"));
            response.put("totalRetiros", datosReporte.get("totalRetiros"));
            response.put("totalCapitalInicial", datosReporte.get("totalCapitalInicial"));
            response.put("capitalFinal", datosReporte.get("capitalFinal"));
            response.put("ecuacionContable", datosReporte.get("ecuacionContable"));
            response.put("fechaInicio", datosReporte.get("fechaInicio"));
            response.put("fechaFin", datosReporte.get("fechaFin"));
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (JsonProcessingException e) {
            response.put("error", "Error al procesar los datos del reporte: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("error", "Error al obtener el reporte: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }
}
