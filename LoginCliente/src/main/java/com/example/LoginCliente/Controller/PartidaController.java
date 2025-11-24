package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Models.*;
import com.example.LoginCliente.Service.CuentaService;
import com.example.LoginCliente.Service.PartidaService;
import com.example.LoginCliente.Service.UsuarioService;
import com.example.LoginCliente.Service.DocumentosPartidaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.BigDecimalParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/partidas")
public class PartidaController {

    private static final Logger logger = LoggerFactory.getLogger(PartidaController.class);

    @Autowired
    private PartidaService partidaService;

    @Autowired
    private CuentaService cuentaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private DocumentosPartidaService documentosPartidaService;

    @GetMapping("/libro-diario")
    public String librodiario(Model model, HttpSession session) throws JsonProcessingException {
        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
        if (empresaActiva == null) {
            return "redirect:/empresas/mis-empresas";
        }

        List<Partida> partidas = partidaService.findByIdEmpresa(empresaActiva);
        List<Cuenta> cuentas = cuentaService.findByIdEmpresa(empresaActiva);

        Map<PartidaDTO, List<Movimiento>> partidasConMovimientos = new LinkedHashMap<>();
        Map<Integer, List<DocumentosFuenteDTO>> documentosPorPartida = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Partida partida : partidas) {
            String fechaFormateada = "";
            if (partida.getFecha() != null) {
                if (partida.getFecha() instanceof Timestamp) {
                    fechaFormateada = ((Timestamp) partida.getFecha()).toLocalDateTime().format(formatter);
                } else if (partida.getFecha() instanceof java.util.Date) {
                    fechaFormateada = new Timestamp(((java.util.Date) partida.getFecha()).getTime()).toLocalDateTime().format(formatter);
                } else {
                    fechaFormateada = partida.getFecha().toString();
                }
            }
            String autorStr = partida.getAutor() != null ? partida.getAutor().toString() : "";
            List<DocumentosFuente> documentos = documentosPartidaService.findDocumentosByPartidaId(partida.getIdPartida());
            List<DocumentosFuenteDTO> documentosDTO = new ArrayList<>();
            documentos.forEach(documento -> {
                String authorUsuario = documento.getAnadidoPor() != null ? documento.getAnadidoPor().getUsuario() : "";
                String fechaSubida = documento.getFecha_subida() != null ? documento.getFecha_subida().toString() : "";
                documentosDTO.add(
                        new DocumentosFuenteDTO(
                                documento.getId_documento(),
                                documento.getNombre(),
                                documento.getRuta(),
                                fechaSubida,
                                authorUsuario
                        ));
            });
            PartidaDTO partidaDTO = new PartidaDTO(
                    partida.getIdPartida(),
                    partida.getConcepto(),
                    fechaFormateada,
                    autorStr
            );
            List<Movimiento> movimientos = partidaService.findMovimientosByPartida(partida.getIdPartida());
            partidasConMovimientos.put(partidaDTO, movimientos);
            documentosPorPartida.put(partida.getIdPartida(), documentosDTO);
        }

        ObjectMapper mapper = new ObjectMapper();
        String documentosJson = mapper.writeValueAsString(documentosPorPartida);

        model.addAttribute("partidasConMovimientos", partidasConMovimientos);
        model.addAttribute("documentosPorPartida", documentosJson);
        model.addAttribute("cuentas", cuentas);
        model.addAttribute("nuevaPartida", new Partida());
        model.addAttribute("page", "libro-diario");

        return "libro-diario";
    }

    @PostMapping("/crear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearPartida(@RequestParam("concepto") String concepto,
                                                            @RequestParam("fechaPartida") String fechaPartida,
                                                            @RequestParam(value = "movimientos") String movimientosJson,
                                                            @RequestParam("nombresArchivos") String nombresArchivosJson,
                                                            @RequestParam(required = false, defaultValue = "false") boolean forzar,
                                                            @RequestParam(value = "archivosOrigen", required = false) MultipartFile[] archivosOrigen,
                                                            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
            Integer usuarioEmpresaId = (Integer) session.getAttribute("usuarioEmpresaId");

            // Log de depuración para entender por qué puede fallar
            logger.debug("crearPartida called - empresaActiva={}, usuarioEmpresaId={}, concepto='{}', fechaPartida='{}'", empresaActiva, usuarioEmpresaId, concepto, fechaPartida);
            logger.debug("movimientosJson length={}, nombresArchivosJson length={}", movimientosJson != null ? movimientosJson.length() : 0, nombresArchivosJson != null ? nombresArchivosJson.length() : 0);
            logger.debug("archivosOrigen is null? {}", archivosOrigen == null);
            if (archivosOrigen != null) {
                logger.debug("archivosOrigen length={}", archivosOrigen.length);
                for (int i = 0; i < archivosOrigen.length; i++) {
                    MultipartFile f = archivosOrigen[i];
                    logger.debug("archivo[{}] name={}, size={}", i, f.getOriginalFilename(), f.getSize());
                }
            }

            if (empresaActiva == null || usuarioEmpresaId == null) {
                response.put("error", "Debe seleccionar una empresa primero");
                logger.warn("crearPartida aborted - empresaActiva or usuarioEmpresaId is null (empresaActiva={}, usuarioEmpresaId={})", empresaActiva, usuarioEmpresaId);
                return ResponseEntity.badRequest().body(response);
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            Usuario usuario = usuarioService.findByUsuario(username);

            Partida partida = new Partida();
            partida.setConcepto(concepto);
            // Obtener la fecha enviada por el usuario
            if (fechaPartida != null && !fechaPartida.isEmpty()) {
                // Convertir la fecha (formato yyyy-MM-dd) a Timestamp
                LocalDateTime fecha = LocalDateTime.parse(fechaPartida + "T00:00:00");
                partida.setFecha(Timestamp.valueOf(fecha));
            } else {
                partida.setFecha(new Timestamp(System.currentTimeMillis()));
            }
            partida.setAutor(usuario.getIdUsuario());
            partida.setIdEmpresa(empresaActiva);
            partida.setIdUsuarioEmpresa(usuarioEmpresaId);

            List<Movimiento> movimientos = new ArrayList<>();
            var mapper = new ObjectMapper();
            List<Map<String, Object>> movimientosData = mapper.readValue(movimientosJson, List.class);

            BigDecimal totalDebe = BigDecimal.ZERO;
            BigDecimal totalHaber = BigDecimal.ZERO;

            for (Map<String, Object> movData : movimientosData) {
                Movimiento movimiento = new Movimiento();
                movimiento.setIdCuenta(Integer.parseInt(movData.get("idCuenta").toString()));
                movimiento.setMonto(new BigDecimal(movData.get("monto").toString()));
                movimiento.setTipo((String) movData.get("tipo"));
                movimiento.setIdEmpresa(empresaActiva);
                movimiento.setIdUsuarioEmpresa(usuarioEmpresaId);

                if ("D".equals(movimiento.getTipo())) {
                    totalDebe = totalDebe.add(movimiento.getMonto());
                } else {
                    totalHaber = totalHaber.add(movimiento.getMonto());
                }

                movimientos.add(movimiento);
            }

            if (totalDebe.compareTo(totalHaber) != 0) {
                response.put("error", "El total del Debe debe ser igual al total del Haber");
                return ResponseEntity.badRequest().body(response);
            }

            // Validar saldos de las cuentas solo si no se está forzando
            if (!forzar) {
                List<String> cuentasNegativas = validarSaldosCuentas(movimientos);
                if (!cuentasNegativas.isEmpty()) {
                    response.put("warning", true);
                    response.put("cuentasNegativas", cuentasNegativas);
                    response.put("mensaje", "Las siguientes cuentas quedarían con saldo negativo: " + String.join(", ", cuentasNegativas) + ". \n\n¿Desea continuar de todos modos? Deberá realizar ajustes contables.");
                    return ResponseEntity.ok(response);
                }
            }

            String[] nombresArchivos = mapper.readValue(nombresArchivosJson, String[].class);
            logger.debug("nombresArchivos count={}", nombresArchivos != null ? nombresArchivos.length : 0);
            List<DocumentosFuente> documentosFuentes = new ArrayList<>();
            if (archivosOrigen != null) {
                for (int i = 0; i < archivosOrigen.length; i++) {
                    String nombreDbArchivo = nombresArchivos[i];
                    MultipartFile archivoOrigen = archivosOrigen[i];
                    logger.debug("Processing archivo {} -> nombreDbArchivo='{}' originalName='{}'", i, nombreDbArchivo, archivoOrigen.getOriginalFilename());

                    DocumentosFuente documento = new DocumentosFuente();
                    if (archivoOrigen != null && !archivoOrigen.isEmpty()) {
                        String uploadsDir = "uploads/";
                        File dir = new File(uploadsDir);
                        if (!dir.exists()) dir.mkdirs();

                        String nombreArchivo = archivoOrigen.getOriginalFilename();
                        String extension = "";
                        if (nombreArchivo != null && nombreArchivo.contains(".")) {
                            extension = nombreArchivo.substring(nombreArchivo.lastIndexOf("."));
                        }
                        String nombreGuardado = UUID.randomUUID().toString() + extension;
                        Path destino = Paths.get(uploadsDir + nombreGuardado);
                        Files.write(destino, archivoOrigen.getBytes());
                        documento.setNombre(nombreDbArchivo);
                        documento.setRuta(destino.toString());
                        documento.setFecha_subida(partida.getFecha());
                        documento.setAnadidoPor(usuario);

                        documentosFuentes.add(documento);
                    }
                }
            }

            Partida savedPartida = partidaService.save(partida, movimientos, documentosFuentes, empresaActiva);
            response.put("success", true);
            response.put("partidaId", savedPartida.getIdPartida());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error al crear la partida: " + e.getMessage());
            logger.error("Error al crear la partida", e);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{idPartida}/movimientos")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getMovimientos(@PathVariable Integer idPartida, HttpSession session) {
        try {
            Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
            if (empresaActiva == null) {
                return ResponseEntity.badRequest().build();
            }

            List<Movimiento> movimientos = partidaService.findMovimientosByPartida(idPartida);
            List<Cuenta> cuentas = cuentaService.findByIdEmpresa(empresaActiva);

            List<Map<String, Object>> movimientosConNombres = new ArrayList<>();

            for (Movimiento mov : movimientos) {
                Map<String, Object> movData = new HashMap<>();
                movData.put("monto", mov.getMonto());
                movData.put("tipo", mov.getTipo());

                // Find account name
                String nombreCuenta = "";
                for (Cuenta cuenta : cuentas) {
                    if (cuenta.getIdCuenta().equals(mov.getIdCuenta())) {
                        nombreCuenta = cuenta.getNombre();
                        break;
                    }
                }
                movData.put("nombreCuenta", nombreCuenta);

                movimientosConNombres.add(movData);
            }

            return ResponseEntity.ok(movimientosConNombres);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtener datos completos de una partida para edición
     */
    @GetMapping("/{idPartida}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerPartida(@PathVariable Integer idPartida) {
        try {
            Optional<Partida> partidaOpt = partidaService.findById(idPartida);
            if (!partidaOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Partida partida = partidaOpt.get();
            Map<String, Object> response = new HashMap<>();

            // Formatear fecha
            String fechaFormateada = "";
            if (partida.getFecha() != null) {
                if (partida.getFecha() instanceof Timestamp) {
                    fechaFormateada = ((Timestamp) partida.getFecha()).toLocalDateTime().toLocalDate().toString();
                }
            }

            response.put("idPartida", partida.getIdPartida());
            response.put("concepto", partida.getConcepto());
            response.put("fecha", fechaFormateada);

            // Obtener movimientos
            List<Movimiento> movimientos = partidaService.findMovimientosByPartida(idPartida);
            List<Map<String, Object>> movimientosData = new ArrayList<>();
            for (Movimiento mov : movimientos) {
                Map<String, Object> movData = new HashMap<>();
                movData.put("idMovimiento", mov.getIdMovimiento());
                movData.put("idCuenta", mov.getIdCuenta());
                movData.put("monto", mov.getMonto());
                movData.put("tipo", mov.getTipo());
                movimientosData.add(movData);
            }
            response.put("movimientos", movimientosData);

            // Obtener documentos
            List<DocumentosFuente> documentos = documentosPartidaService.findDocumentosByPartidaId(idPartida);
            List<Map<String, Object>> documentosData = new ArrayList<>();
            for (DocumentosFuente doc : documentos) {
                Map<String, Object> docData = new HashMap<>();
                docData.put("id", doc.getId_documento());
                docData.put("nombre", doc.getNombre());
                docData.put("ruta", doc.getRuta());
//                docData.put("valor", doc.getValor());
                documentosData.add(docData);
            }
            response.put("documentos", documentosData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Actualizar una partida existente
     */
    @PostMapping("/actualizar/{idPartida}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarPartida(
            @PathVariable Integer idPartida,
            @RequestParam("concepto") String concepto,
            @RequestParam("fechaPartida") String fechaPartida,
            @RequestParam(value = "movimientos") String movimientosJson,
            @RequestParam(value = "nombresArchivos", required = false) String nombresArchivosJson,
            @RequestParam(value = "archivosOrigen", required = false) MultipartFile[] archivosOrigen,
            @RequestParam(value = "montosArchivo", required = false) String montosArchivoJson,
            @RequestParam(value = "documentosAEliminar", required = false) String documentosAEliminarJson,
            @RequestParam(required = false, defaultValue = "false") boolean forzar,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
            Integer usuarioEmpresaId = (Integer) session.getAttribute("usuarioEmpresaId");

            if (empresaActiva == null || usuarioEmpresaId == null) {
                response.put("error", "Debe seleccionar una empresa primero");
                return ResponseEntity.badRequest().body(response);
            }

            // Verificar que la partida existe
            Optional<Partida> partidaOpt = partidaService.findById(idPartida);
            if (!partidaOpt.isPresent()) {
                response.put("error", "Partida no encontrada");
                return ResponseEntity.notFound().build();
            }

            Partida partida = partidaOpt.get();

            // Actualizar datos básicos
            partida.setConcepto(concepto);
            if (fechaPartida != null && !fechaPartida.isEmpty()) {
                LocalDateTime fecha = LocalDateTime.parse(fechaPartida + "T00:00:00");
                partida.setFecha(Timestamp.valueOf(fecha));
            }

            // Procesar movimientos
            List<Movimiento> movimientos = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> movimientosData = mapper.readValue(movimientosJson, List.class);

            BigDecimal totalDebe = BigDecimal.ZERO;
            BigDecimal totalHaber = BigDecimal.ZERO;

            for (Map<String, Object> movData : movimientosData) {
                Integer idCuenta = Integer.valueOf(movData.get("idCuenta").toString());
                BigDecimal monto = new BigDecimal(movData.get("monto").toString());
                String tipo = movData.get("tipo").toString();

                Movimiento mov = new Movimiento();
                mov.setIdCuenta(idCuenta);
                mov.setMonto(monto);
                mov.setTipo(tipo);
                mov.setIdPartida(idPartida);

                movimientos.add(mov);

                if ("D".equals(tipo)) {
                    totalDebe = totalDebe.add(monto);
                } else {
                    totalHaber = totalHaber.add(monto);
                }
            }

            // Validar balance
            if (totalDebe.compareTo(totalHaber) != 0) {
                response.put("error", "El total del Debe debe ser igual al total del Haber");
                return ResponseEntity.badRequest().body(response);
            }

            // Validar saldos de las cuentas solo si no se está forzando
            if (!forzar) {
                // Crear lista de movimientos temporal para validación
                // Necesitamos restar los movimientos antiguos y sumar los nuevos
                List<String> cuentasNegativas = validarSaldosCuentasConEdicion(idPartida, movimientos);
                if (!cuentasNegativas.isEmpty()) {
                    response.put("warning", true);
                    response.put("cuentasNegativas", cuentasNegativas);
                    response.put("mensaje", "Las siguientes cuentas quedarían con saldo negativo: " + String.join(", ", cuentasNegativas) + ". \n\n ¿Desea continuar de todos modos? Deberá realizar ajustes contables.");
                    return ResponseEntity.ok(response);
                }
            }

            // Eliminar documentos marcados
            if (documentosAEliminarJson != null && !documentosAEliminarJson.isEmpty()) {
                List<Integer> documentosAEliminar = mapper.readValue(documentosAEliminarJson, List.class);
                for (Integer docId : documentosAEliminar) {
                    documentosPartidaService.deleteDocumentoById(docId);
                }
            }

            // Procesar nuevos documentos
            List<DocumentosFuente> nuevosDocumentos = new ArrayList<>();
            if (archivosOrigen != null && archivosOrigen.length > 0 && nombresArchivosJson != null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication.getName();
                Usuario usuario = usuarioService.findByUsuario(username);

                List<String> nombresArchivos = mapper.readValue(nombresArchivosJson, List.class);
                List<BigDecimal> montosArchivo = mapper.readValue(montosArchivoJson, List.class);

                String uploadDir = "uploads/";
                File uploadDirFile = new File(uploadDir);
                if (!uploadDirFile.exists()) {
                    uploadDirFile.mkdirs();
                }

                for (int i = 0; i < archivosOrigen.length; i++) {
                    MultipartFile file = archivosOrigen[i];
                    if (file != null && !file.isEmpty()) {
                        String originalFilename = file.getOriginalFilename();
                        String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".bin";
                        String nuevoNombre = UUID.randomUUID() + extension;
                        Path filepath = Paths.get(uploadDir, nuevoNombre);
                        Files.write(filepath, file.getBytes());

                        DocumentosFuente documento = new DocumentosFuente();
                        documento.setNombre(nombresArchivos.get(i));
                        documento.setRuta("uploads/" + nuevoNombre);
                        documento.setFecha_subida(new Timestamp(System.currentTimeMillis()));
                        documento.setAnadidoPor(usuario);

                        nuevosDocumentos.add(documento);
                    }
                }
            }

            // Actualizar partida con movimientos y documentos
            Partida partidaActualizada = partidaService.update(partida, movimientos, nuevosDocumentos);

            response.put("success", true);
            response.put("partidaId", partidaActualizada.getIdPartida());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error al actualizar la partida: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarPartida(@PathVariable Integer id,
                                  @RequestHeader(value = "Referer", required = false) String referer,
                                  RedirectAttributes redirectAttributes) {
        try {
            partidaService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Partida eliminada exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }

        // Si hay referer, redirige ahí, sino a una página por defecto
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        // Sino encuentra la referencia, redirige al dashboard o a otra página predeterminada
        return "redirect:/dashboard";
    }

    /**
     * Valida si los movimientos propuestos dejarían alguna cuenta con saldo negativo
     * @param movimientos Lista de movimientos a validar
     * @return Lista de nombres de cuentas que quedarían en negativo
     */
    private List<String> validarSaldosCuentas(List<Movimiento> movimientos) {
        List<String> cuentasNegativas = new ArrayList<>();

        // Agrupar movimientos por cuenta
        Map<Integer, BigDecimal> cambiosPorCuenta = new HashMap<>();

        for (Movimiento mov : movimientos) {
            Integer idCuenta = mov.getIdCuenta();
            BigDecimal cambio = cambiosPorCuenta.getOrDefault(idCuenta, BigDecimal.ZERO);

            // Obtener la cuenta para conocer su naturaleza
            Cuenta cuenta = cuentaService.findById(idCuenta)
                    .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));

            // Calcular el impacto en el saldo según naturaleza de la cuenta
            if ("D".equals(cuenta.getNaturaleza())) {
                // Para cuentas Deudoras: Debe aumenta, Haber disminuye
                if ("D".equals(mov.getTipo())) {
                    cambio = cambio.add(mov.getMonto());
                } else {
                    cambio = cambio.subtract(mov.getMonto());
                }
            } else {
                // Para cuentas Acreedoras: Haber aumenta, Debe disminuye
                if ("H".equals(mov.getTipo())) {
                    cambio = cambio.add(mov.getMonto());
                } else {
                    cambio = cambio.subtract(mov.getMonto());
                }
            }

            cambiosPorCuenta.put(idCuenta, cambio);
        }

        // Verificar cada cuenta afectada
        for (Map.Entry<Integer, BigDecimal> entry : cambiosPorCuenta.entrySet()) {
            Integer idCuenta = entry.getKey();
            BigDecimal cambio = entry.getValue();

            // Obtener saldo actual
            Map<String, BigDecimal> saldos = cuentaService.calcularSaldoCuenta(idCuenta);
            BigDecimal saldoActual = saldos.get("saldo");
            BigDecimal saldoProyectado = saldoActual.add(cambio);

            // Si el saldo proyectado es negativo, agregar a la lista
            if (saldoProyectado.compareTo(BigDecimal.ZERO) < 0) {
                Cuenta cuenta = cuentaService.findById(idCuenta).orElse(null);
                if (cuenta != null) {
                    cuentasNegativas.add(cuenta.getNombre() + " (Saldo actual: $" + saldoActual +
                                        ", Saldo proyectado: $" + saldoProyectado + ")");
                }
            }
        }

        return cuentasNegativas;
    }

    /**
     * Valida saldos cuando se está editando una partida existente
     * Primero resta el efecto de los movimientos antiguos, luego suma los nuevos
     */
    private List<String> validarSaldosCuentasConEdicion(Integer idPartida, List<Movimiento> nuevosMovimientos) {
        List<String> cuentasNegativas = new ArrayList<>();

        // Obtener movimientos antiguos
        List<Movimiento> movimientosAntiguos = partidaService.findMovimientosByPartida(idPartida);

        // Agrupar cambios por cuenta
        Map<Integer, BigDecimal> cambiosPorCuenta = new HashMap<>();

        // Restar efecto de movimientos antiguos
        for (Movimiento mov : movimientosAntiguos) {
            Integer idCuenta = mov.getIdCuenta();
            BigDecimal cambio = cambiosPorCuenta.getOrDefault(idCuenta, BigDecimal.ZERO);

            Cuenta cuenta = cuentaService.findById(idCuenta)
                    .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));

            // Restar el efecto anterior (lo contrario de sumar)
            if ("D".equals(cuenta.getNaturaleza())) {
                if ("D".equals(mov.getTipo())) {
                    cambio = cambio.subtract(mov.getMonto()); // Restar lo que se había sumado
                } else {
                    cambio = cambio.add(mov.getMonto()); // Sumar lo que se había restado
                }
            } else {
                if ("H".equals(mov.getTipo())) {
                    cambio = cambio.subtract(mov.getMonto());
                } else {
                    cambio = cambio.add(mov.getMonto());
                }
            }

            cambiosPorCuenta.put(idCuenta, cambio);
        }

        // Sumar efecto de nuevos movimientos
        for (Movimiento mov : nuevosMovimientos) {
            Integer idCuenta = mov.getIdCuenta();
            BigDecimal cambio = cambiosPorCuenta.getOrDefault(idCuenta, BigDecimal.ZERO);

            Cuenta cuenta = cuentaService.findById(idCuenta)
                    .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));

            if ("D".equals(cuenta.getNaturaleza())) {
                if ("D".equals(mov.getTipo())) {
                    cambio = cambio.add(mov.getMonto());
                } else {
                    cambio = cambio.subtract(mov.getMonto());
                }
            } else {
                if ("H".equals(mov.getTipo())) {
                    cambio = cambio.add(mov.getMonto());
                } else {
                    cambio = cambio.subtract(mov.getMonto());
                }
            }

            cambiosPorCuenta.put(idCuenta, cambio);
        }

        // Verificar cada cuenta afectada
        for (Map.Entry<Integer, BigDecimal> entry : cambiosPorCuenta.entrySet()) {
            Integer idCuenta = entry.getKey();
            BigDecimal cambio = entry.getValue();

            // Obtener saldo actual
            Map<String, BigDecimal> saldos = cuentaService.calcularSaldoCuenta(idCuenta);
            BigDecimal saldoActual = saldos.get("saldo");
            BigDecimal saldoProyectado = saldoActual.add(cambio);

            // Si el saldo proyectado es negativo, agregar a la lista
            if (saldoProyectado.compareTo(BigDecimal.ZERO) < 0) {
                Cuenta cuenta = cuentaService.findById(idCuenta).orElse(null);
                if (cuenta != null) {
                    cuentasNegativas.add(cuenta.getNombre() + " (Saldo actual: $" + saldoActual +
                                        ", Saldo proyectado: $" + saldoProyectado + ")");
                }
            }
        }

        return cuentasNegativas;
    }

}
