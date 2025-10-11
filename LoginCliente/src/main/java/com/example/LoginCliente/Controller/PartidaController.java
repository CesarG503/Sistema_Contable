package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Models.*;
import com.example.LoginCliente.Service.CuentaService;
import com.example.LoginCliente.Service.PartidaService;
import com.example.LoginCliente.Service.UsuarioService;
import com.fasterxml.jackson.core.io.BigDecimalParser;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @GetMapping("/libro-diario")
    public String librodiario(Model model) {
        List<Partida> partidas = partidaService.findAll();
        List<Cuenta> cuentas = cuentaService.findAll();

        Map<PartidaDTO, List<Movimiento>> partidasConMovimientos = new LinkedHashMap<>();
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
            PartidaDTO partidaDTO = new PartidaDTO(
                    partida.getIdPartida(),
                    partida.getConcepto(),
                    fechaFormateada,
                    autorStr
            );
            List<Movimiento> movimientos = partidaService.findMovimientosByPartida(partida.getIdPartida());
            partidasConMovimientos.put(partidaDTO, movimientos);
        }

        model.addAttribute("partidasConMovimientos", partidasConMovimientos);
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
                                                            @RequestParam(value = "archivoOrigen") MultipartFile archivoOrigen,
                                                            @RequestParam("montoArchivo") String montoArchivo) {
        Map<String, Object> response = new HashMap<>();
        System.out.println("concepto");
        try {
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
            partida.setAutor(usuario.getId_usuario());

            List<Movimiento> movimientos = new ArrayList<>();
            var mapper = new ObjectMapper();
            List<Map<String, Object>> movimientosData = mapper.readValue(movimientosJson, List.class);

            BigDecimal totalDebe = BigDecimal.ZERO;
            BigDecimal totalHaber = BigDecimal.ZERO;

            for (Map<String, Object> movData : movimientosData) {
                Movimiento movimiento = new Movimiento();
                movimiento.setId_cuenta(Integer.parseInt(movData.get("idCuenta").toString()));
                movimiento.setMonto(new BigDecimal(movData.get("monto").toString()));
                movimiento.setTipo((String) movData.get("tipo"));

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

            //Si se envió un archivo, guardarlo
            DocumentosFuente documento = new DocumentosFuente();
            if (archivoOrigen != null && !archivoOrigen.isEmpty()) {
                String uploadsDir = "uploads/";
                File dir = new File(uploadsDir);
                if (!dir.exists()) dir.mkdirs();

                String nombreArchivo = archivoOrigen.getOriginalFilename();
                String extension = "";
                if(nombreArchivo != null && nombreArchivo.contains(".")) {
                    extension = nombreArchivo.substring(nombreArchivo.lastIndexOf("."));
                }
                String nombreGuardado = UUID.randomUUID().toString() + extension;
                Path destino = Paths.get(uploadsDir + nombreGuardado);
                Files.write(destino, archivoOrigen.getBytes());
                documento.setRuta(destino.toString());
                documento.setFecha_subida(partida.getFecha());
                documento.setAñadido_por(usuario.getId_usuario());
                BigDecimal valor = BigDecimalParser.parse(montoArchivo);
                documento.setValor(valor);
            }

            List<DocumentosFuente> documentosFuentes = new ArrayList<>();
            documentosFuentes.add(documento);

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
    public ResponseEntity<List<Map<String, Object>>> getMovimientos(@PathVariable Integer idPartida) {
        try {
            List<Movimiento> movimientos = partidaService.findMovimientosByPartida(idPartida);
            List<Cuenta> cuentas = cuentaService.findAll();

            List<Map<String, Object>> movimientosConNombres = new ArrayList<>();

            for (Movimiento mov : movimientos) {
                Map<String, Object> movData = new HashMap<>();
                movData.put("monto", mov.getMonto());
                movData.put("tipo", mov.getTipo());

                // Find account name
                String nombreCuenta = "";
                for (Cuenta cuenta : cuentas) {
                    if (cuenta.getId_cuenta().equals(mov.getId_cuenta())) {
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
