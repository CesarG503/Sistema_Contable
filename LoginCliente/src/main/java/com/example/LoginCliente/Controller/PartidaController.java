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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
                documentosDTO.add(
                        new DocumentosFuenteDTO(
                                documento.getId_documento(),
                                documento.getNombre(),
                                documento.getRuta(),
                                documento.getFecha_subida().toString(),
                                documento.getValor(),
                                documento.getAñadidoPor().getUsuario()
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
                                                            @RequestParam(value = "archivosOrigen", required = false) MultipartFile[] archivosOrigen,
                                                            @RequestParam("montosArchivo") String montosArchivoJson) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
            Integer usuarioEmpresaId = (Integer) session.getAttribute("usuarioEmpresaId");

            if (empresaActiva == null || usuarioEmpresaId == null) {
                response.put("error", "Debe seleccionar una empresa primero");
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

            String[] nombresArchivos = mapper.readValue(nombresArchivosJson, String[].class);
            String[] montosArchivo = mapper.readValue(montosArchivoJson, String[].class);
            List<DocumentosFuente> documentosFuentes = new ArrayList<>();
            if (archivosOrigen != null) {
                for (int i = 0; i < archivosOrigen.length; i++) {
                    String nombreDbArchivo = nombresArchivos[i];
                    MultipartFile archivoOrigen = archivosOrigen[i];
                    String montoArchivo = montosArchivo[i];
                    System.out.println("Nombre para DB: " + nombreDbArchivo);

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
                        documento.setAñadidoPor(usuario);
                        BigDecimal valor = BigDecimalParser.parse(montoArchivo);
                        documento.setValor(valor);

                        documentosFuentes.add(documento);
                    }
                }
            }

            Partida savedPartida = partidaService.save(partida, movimientos, documentosFuentes);
            response.put("success", true);
            response.put("partidaId", savedPartida.getIdPartida());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error al crear la partida: " + e.getMessage());
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
}
